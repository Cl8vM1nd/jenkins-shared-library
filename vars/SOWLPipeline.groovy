#!/usr/bin/env groovy

def call(body) 
{
    // Get all data from body
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def phpImage            = null
    def mysqlImage          = null
    String phpImageName     = 'php-fpm'
    String mysqlImageName   = 'mysql'
    def image               = new image()
    def deploy              = new deploy()
    def appUrl              = "http://${image.getBranchName()}${deploy.getStgDomain()}"

    node(config.node) {
        if(config.parameters) {
            properties([
                // Add parameters is they exist
                parameters ([
                    booleanParam(defaultValue: config.parameters.defaultValue, description: '', name: config.parameters.name)
                ]),
                // Do not run parallel builds. Wait until first finish
                disableConcurrentBuilds()
            ])
        } else {
            // Do not run parallel builds. Wait until first finish
             properties([disableConcurrentBuilds()]);
        }

        try {
            stage('Prepare \u2776') {
                checkout scm
                sh('gcloud docker -a')
                sh("sed -i.bak 's#DB_HOST=sowl-dev-mysql#DB_HOST=${image.getBranchName()}-mysql#' ${workspace}/.env.kubernetes")
                sh("sed -i.bak 's#DB_EMAILS_HOST=sowl-dev-mysql#DB_EMAILS_HOST=${image.getBranchName()}-mysql#' ${workspace}/.env.kubernetes")
                if(config.replaceAppUrl) {
                    sh("sed -i.bak 's#APP_URL=willBeChanged#APP_URL=${appUrl}#' ${workspace}/.env.kubernetes")
                }
            }

            stage('Build Docker images \u2756') {
                ansiColor {
                    phpImage = image.build(
                        config.cluster,
                        config.appName,
                        phpImageName,
                        image.getBranchName()
                    )
                    // Copying startup dump
                    sh("cp -f ${workspace}/tests/scripts/testing_sowl.schema.sql ${deploy.getKubePath()}/docker/mysql/testing_sowl.schema.sql")
                    mysqlImage = image.build(
                        config.cluster,
                        config.appName,
                        mysqlImageName,
                        image.getBranchName(),
                        true,
                        "${deploy.getKubePath()}/docker/mysql"
                    )
                }
            }

            stage('Deploy to staging \u2705') {
                ansiColor {
                    withCredentials([file(credentialsId: "${config.service_account_key_id}", variable: 'SERVICE_ACCOUNT_KEY')]) {
                        sh("gcloud auth activate-service-account ${config.serviceAccount} --key-file ${SERVICE_ACCOUNT_KEY} --project ${config.project}")
                        withEnv(["GOOGLE_APPLICATION_CREDENTIALS=${SERVICE_ACCOUNT_KEY}"]) {
                            image.push(phpImage.tag)
                            image.push(mysqlImage.tag)
                            //if(${params.userFlag})
                            deploy.start(
                                config.cluster,
                                config.zone,
                                config.namespace,
                                config.chartName,
                                [ 'app': image.getBranchName(),
                                  'containers.phpfpm.tag': phpImage.tagNumber,
                                  'containers.phpfpm.repository': phpImage.tagRepo,
                                  'mysql.image': mysqlImage.tagRepo,
                                  'mysql.imageTag': mysqlImage.tagNumber
                                ],
                                [ 'replaceChartName': true]
                            )
                            sh("sleep ${config.waitTime}");
                            deploy.checkPodsAlive(image.getBranchName())
                        }
                    }
                }
            }
        } catch (err) {
            currentBuild.result = 'FAILED'
            throw err
        }
    }
    return this
}
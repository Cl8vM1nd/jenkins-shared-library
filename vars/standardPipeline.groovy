#!/usr/bin/env groovy

def call(body) 
{
    // Get all data from body
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def finalImage      = null
    String phpImageName = 'php-fpm'
    def image           = new image()
    def deploy          = new deploy()

    node(config.node) {
        // Do not run parallel builds. Wait until first finish
        properties([disableConcurrentBuilds()])

        try {
            stage('Prepare \u2776') {
                checkout scm
                sh('gcloud docker -a')
            }

            stage('Build Docker images \u2756') {
                ansiColor {
                    // Set flag if production
                    if(BRANCH_NAME == 'master') {
                        println "PRODUCTION build"
                        sh("export PRODUCTION=true")
                    } else {
                        println "DEVELOP build"
                    }
                    finalImage = image.build(
                        config.cluster,
                        config.appName,
                        phpImageName
                    )
                }
            }

            stage('Deploy to staging \u2705') {
                ansiColor {
                    withCredentials([file(credentialsId: "${config.service_account_key_id}", variable: 'SERVICE_ACCOUNT_KEY')]) {
                        sh("gcloud auth activate-service-account ${config.serviceAccount} --key-file ${SERVICE_ACCOUNT_KEY} --project ${config.project}")
                        withEnv(["GOOGLE_APPLICATION_CREDENTIALS=${SERVICE_ACCOUNT_KEY}"]) {
                            image.push(finalImage.imageTag)
                            deploy.start(
                                config.cluster,
                                config.zone,
                                config.namespace,
                                config.chartName,
                                [ app: config.appName,
                                  'containers.phpfpm.tag': finalImage.imageTagNumber,
                                  'containers.phpfpm.repository': finalImage.imageTagRepo]
                            )
                            sh("sleep ${config.waitTime}")
                            deploy.checkPodsAlive(config.appName)
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
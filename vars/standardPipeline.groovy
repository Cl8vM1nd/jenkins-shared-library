#!/usr/bin/env groovy
/**
* One Image standart Pipeline
*/

def call(body) 
{
    // Get all data from body
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def buildImage      = null
    def image           = new image()
    def deploy          = new deploy()
    def slack           = new slack()

    node(config.node) {
        // Do not run parallel builds. Wait until first finish
        properties([disableConcurrentBuilds()])

        try {
            if(config.slack) {
                slack.send(config.slackChannel, 'STARTED', config.slackMsg, config.slackSuccessProdMsg, config.slackSuccessDevMsg, config.slackFailedMsg)
            }
            stage('Prepare \u2776') {
                checkout scm
                sh('gcloud docker -a')
            }

            stage('Build Docker images \u2756') {
                ansiColor {
                    buildImage = image.build(
                        config.cluster,
                        config.appName,
                        config.imageName
                    )
                }
            }

            stage('Deploy to staging \u2705') {
                ansiColor {
                    withCredentials([file(credentialsId: "${config.service_account_key_id}", variable: 'SERVICE_ACCOUNT_KEY')]) {
                        sh("gcloud auth activate-service-account ${config.serviceAccount} --key-file ${SERVICE_ACCOUNT_KEY} --project ${config.project}")
                        withEnv(["GOOGLE_APPLICATION_CREDENTIALS=${SERVICE_ACCOUNT_KEY}"]) {
                            image.push(buildImage.tag)
                            def helm = deploy.setHelmParams(config.helmParams, buildImage, ['serviceAccountKey' : "${SERVICE_ACCOUNT_KEY}"])
                            deploy.start(config, helm)
                            sh("sleep ${config.waitTime}");
                            deploy.checkPodsAlive(config.chartName)
                        }
                    }
                }
                if(config.slack) {
                    slack.send(config.slackChannel, 'SUCCESS', config.slackMsg, config.slackSuccessProdMsg, config.slackSuccessDevMsg, config.slackFailedMsg)
                }
            }
        } catch (err) {
            if(config.slack) {
                slack.send(config.slackChannel, 'FAILED', config.slackMsg, config.slackSuccessProdMsg, config.slackSuccessDevMsg, config.slackFailedMsg)
            }
            currentBuild.result = 'FAILED'
            throw err
        }
    }
    return this
}
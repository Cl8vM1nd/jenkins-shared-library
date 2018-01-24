#!/usr/bin/env groovy

def call(body) 
{
    // Build status of null means success.
    buildStatus = buildStatus ?: 'SUCCESS'

    def color
    def job_name = BRANCH_NAME.replace("%2F", "_")
    def msg = "${buildStatus}: `${job_name}`"
    def domainIp = "http://${ip}".replace("\n", "")

    if (buildStatus == 'STARTED') {
        color = '#D4DADF'
    } else if (buildStatus == 'SUCCESS') {
        color = '#69ff77'
        msg = "Deployed `${job_name}`"
    } else if (buildStatus == 'UNSTABLE') {
        color = '#FFFE89'
    } else {
        color = '#FF9FA1'
    }

    if (buildStatus == 'FAILED' || buildStatus == 'UNSTABLE') {
      slackSend(color: color, channel: '#development', message: ":docker-down:  ${msg}\n${env.BUILD_URL}")
    } else if (buildStatus == 'SUCCESS') {
      slackSend(color: color, channel: '#development', message: ":docker: Dear Sowlcom-Team \n${msg} to\n ${domainIp} or ${url}\nHappy Testing!")
    } else {
      slackSend(color: color, channel: '#development', message: ":docker: ${msg}")
    }
}
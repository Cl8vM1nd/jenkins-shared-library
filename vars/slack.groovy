#!/usr/bin/env groovy

/**
* Send mesasge through slack
* @param String channel
* @param String buildStatus
* @param String msg
* @param String successProdMsg
* @param String successDevMsg
* @param String failedMsg
*/
def send(
    String channel          = '#development', 
    String buildStatus      = 'STARTED', 
    String msg              = 'Build started.', 
    String successProdMsg   = 'Success production build.', 
    String successDevMsg    = 'Success develop build.', 
    String failedMsg        = 'Failed build.') 
{
    // Build status of null means success.
    buildStatus = buildStatus ?: 'SUCCESS'

    def color

    if (buildStatus == 'STARTED') {
        color = '#D4DADF'
    } else if (buildStatus == 'SUCCESS') {
        color = '#69ff77'
    } else if (buildStatus == 'UNSTABLE') {
        color = '#FFFE89'
    } else {
        color = '#FF9FA1'
    }

    if (buildStatus == 'FAILED' || buildStatus == 'UNSTABLE') {
      slackSend(color: color, channel: channel, message: failedMsg)
    } else if (buildStatus == 'SUCCESS') {
        if(BRANCH_NAME == 'master') {
            slackSend(color: color, channel: channel, message: successProdMsg)
        } else {
            slackSend(color: color, channel: channel, message: successDevMsg)
        }
    } else {
      slackSend(color: color, channel: channel, message: msg)
    }
}

return this
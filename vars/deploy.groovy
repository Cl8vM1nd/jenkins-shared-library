#!/usr/bin/env groovy

String getScriptPath()
{
    return "${workspace}/kube/scripts"
}

String getChartPath()
{
    return "${workspace}/kube/charts/"
}

/**
* Setting cluster
* @param String cluster
* @param String zone
* @param String namespace
*/
def setCluster(String cluster, String zone, String namespace)
{
    sh("gcloud container clusters get-credentials ${cluster} --zone=${zone}")
    sh("kubectl config set-context \$(kubectl config current-context) --namespace=${namespace}")
}

/**
* Deploy an app
* @param String cluster
* @param String zone
* @param String namespace
* @param String chartName
* @param helmData
*/
def start(String cluster, String zone, String namespace, String chartName, helmData)
{
    setCluster(cluster, zone, namespace)

    // Replace chart version
    sh("sed -i.bak 's#version: [0-9]*#version: ${env.BUILD_NUMBER}#' ${getChartPath() + chartName}/Chart.yaml")
    installOrUpdateHelmChart(getChartPath() + chartName, namespace, helmData)
    println "Waiting for pods to be up..."
}

/**
* Install or update helm chart
* @param String chartPath
* @param String namespace
* @param Map vars
*/
def installOrUpdateHelmChart(String chartPath, String namespace, vars)
{
    String scriptName = 'helmStartUpdate'
    def helmData = prepareHelmData(vars)

    downloadScripts()
    sh("sh ${getScriptPath()}/bash/${scriptName}.sh ${vars.app} ${namespace} ${chartPath} '${helmData}'")
}

/**
* Check if pods is alive
*/
def checkPodsAlive(String appName)
{
     String scriptName = 'podAvailability'
     sh("sh ${getScriptPath()}/bash/${scriptName}.sh ${appName}")
}

/**
* Prepare map data to be passed to helm
* @param Map vars
*/
def prepareHelmData(vars)
{
    def helmData = '--set '
    for (e in vars) {
        if(vars.values()[vars.size() - 1] == e.value) {
            helmData += "${e.key}=${e.value}"
        } else {
            helmData += "${e.key}=${e.value},"
        }
    }
    return helmData
}

/**
* Download bash scripts
*/
def downloadScripts()
{
    def branch = 'develop'
    sh("mkdir -p ${getScriptPath()}")
    dir("${getScriptPath()}") {
        // Get bash scripts
        git branch: branch, credentialsId: 'Gitlab Deploy Key', url: 'git@gitlab.com:sowl-tech/jenkins-shared-libs.git'   
    }
}

return this
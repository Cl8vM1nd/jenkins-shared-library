#!/usr/bin/env groovy

/**
* Return version of image
*/
String getVersion()
{
     return "v${env.BUILD_NUMBER}"
}

/**
* Return image repo path
* @param String cluster
* @param String appName
* @param String imageName
*/
def getImageRepo(String cluster, String appName, String imageName)
{
    return "gcr.io/${cluster}/${appName}/${imageName}"
}

/**
* Building image
* @param cluster
* @param appName
* @param imageName
* @return map
*/
def build(String cluster, String appName, String imageName)
{
    String imageTagRepo = getImageRepo(cluster, appName, imageName)
    String imageTagNumber = getVersion()
    String imageTag = "${imageTagRepo}:${imageTagNumber}"
    println "Building ${imageName} image"
    sh("docker build -t ${imageTag} ${workspace}")
    return [imageTagRepo: imageTagRepo, imageTagNumber: getVersion(), imageTag: imageTag]
}

/**
* Pushing image
* @param imageTag
**/
def push(String imageTag)
{
    println "Pushing image ${imageTag}"
    sh("gcloud docker -- push ${imageTag}")
}

return this
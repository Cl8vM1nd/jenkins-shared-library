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
def getImageRepo(String cluster, String appName, String imageName, String branchName = null)
{
    return branchName ? "gcr.io/${cluster}/${appName}/${branchName}/${imageName}" : "gcr.io/${cluster}/${appName}/${imageName}"
}

/**
* Building image
* @param cluster
* @param appName
* @param imageName
* @param branchName
* @param noCache
* @param buildPath
* @return map
*/
def build(String cluster, String appName, String imageName, String branchName = null, boolean noCache = false, String buildPath = '.')
{
    String imageTagRepo = getImageRepo(cluster, appName, imageName, branchName)
    String imageTagNumber = getVersion()
    String imageTag = "${imageTagRepo}:${imageTagNumber}"
    println "Building ${imageName} image"
    noCache ? sh("cd '${workspace}' && docker build --no-cache -t '${imageTag}' '${buildPath}'") : sh("cd '${workspace}' && docker build -t '${imageTag}' '${buildPath}'")
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

/**
* Returning correct branchName
* @param imageTag
* @return String
**/
String getBranchName()
{
    String branchName = BRANCH_NAME
    // Shorten the name of the branch if biggger then 30 symbols
    if (branchName.length() > 30) {
        branchName = branchName.take(30)
    }
    return branchName
           .replace('feature', '')
           .replace('hotfix', '')
           .replace('release', '')
           .replace('+', '')
           .replace('_', '-')
           .replace('/', '')
           .toLowerCase()
}

return this
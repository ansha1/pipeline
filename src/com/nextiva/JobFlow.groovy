package com.nextiva




def call(Map config) {






    stage("Checkout") {
        container(jnlp) {
            checkout scm
        }
    }
    stage("Build version verification") {   //if on DEV,develop,master......
        container(build) {
            checkversion(config)

            config.check
            if ()
        }
    }
    stage("Unit testing") {  //Dev release PR    master if trunk base
        container(build) {

        }
    }

    stage("Sonar analysing") {   //Dev brannh only
        container(build) {

        }
    }
    stage("Build artifact") {
        container(build) {

        }
    }
    stage("Build docker image") {
        container(build) {

        }
    }
    stage("Publish artifact") {
        container(build) {

        }
    }
    stage("Publish docker image") {
        container(build) {

        }
    }
    stage("Veracode security scan") {
        container(jnlp) {

        }
    }
    stage("Tennable security scan") {
        container(build) {

        }
    }
    stage("Integration tests") {  //only on the PR branch
        container(kubernetes) {

        }
    }
    stage("Deploy to the environment") {  //Only on dev/develop/release/master/hotfix
        container(kubernetes) {

        }
    }

    stage("Service healthcheck and version validation") {
        container(jnlp) {

        }
        //Newrelic info
        //Prometheus build info
    }
    stage("Post deploy stage") {
        container(build) {

        }
        //Newrelic info
        //Prometheus build info
    }
    stage("QA core team tests") {
        container(jnlp) {

        }
        //Newrelic info
        //Prometheus build info
    }
    stage("Send notifications") {
        container(jnlp) {

        }
    }
    return this
}





//dockerTemplate {
//
//    commonConfig {
//        channelToNotify          // gitflow\trunkbase
//        appName
//        branchingModel
//    }
//
//    slaveConfig {
//        extraEnvs
//        slaveName = podTemplateConfiguration.get("slaveName", "slave")
//        buildNamespace = podTemplateConfiguration.get("buildNamespace", "jenkins")
//        image = podTemplateConfiguration.get("image")
//        resourceRequestCpu = podTemplateConfiguration.get("resourceRequestCpu", "250m")
//        resourceRequestMemory = podTemplateConfiguration.get("resourceRequestMemory", "1Gi")
//        buildDaysToKeepStr = podTemplateConfiguration.get("buildDaysToKeepStr", "3")
//        buildNumToKeepStr = podTemplateConfiguration.get("buildNumToKeepStr", "5")
//        jobTimeoutMinutes = podTemplateConfiguration.get("jobTimeoutMinutes", "60")
//        paramlist = podTemplateConfiguration.get("paramlist", []) + [booleanParam(name: 'DEBUG', description: 'Enable DEBUG mode with extended output', defaultValue: false)]
//        authMap = podTemplateConfiguration.get("auth", [:])
//        propertiesList = [parameters(paramlist), buildDiscarder(logRotator(daysToKeepStr: buildDaysToKeepStr, numToKeepStr: buildNumToKeepStr)), authorizationMatrix(inheritanceStrategy: nonInheriting(), permissions: securityPermissions)]
//        isDisableConcurrentBuildsEnabled
//    }
//
//    buildConfig {
//        language
//        pathToSrc
//    }
//
//    integrationTestsConfig{
//
//        dependenciesList
//    }
//
//    deployConfig {
//        kubernetesClusterMap
//        Healthcheck
//        postDeployStep{mymethod()}
//    }
//
//    securityConfig{
//        allowedUsersPerBranch dev s
//    }
//
//}

//Context_variables
//extraenvs

//BUILD INFO
//appname
//BuildName

//Build Configuration
//jobTimeoutMinutes
//buildNumToKeepStr
//artifactNumToKeepStr

//FLOW CONFIG
//gitflow \ trunkbase

//BUILD_CONFIG
//publishBuildArtifact
//publishDockerImage

//DEPLOY INFO
//kubernetesCluster
//Healthcheck

//NOTIFICAION
//channeltonotify
//defaultSlackNotificationMap ?
//        slackNotifictionScope ?

//SECURITY
//                branchPermissionsMap


//                node label
NEWRELIC_APP_ID_MAP
isVeracodeScanEnabled
veracodeApplicationScope

//LANGUAGE
//BUILDIMAGE
//utils


build_version
ishotfixdeploy





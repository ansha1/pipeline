package com.nextiva


dockerTemplate {

    commonConfig {
        channelToNotify          // gitflow\trunkbase
        appName
        branchingModel
    }

    slaveConfig {
        extraEnvs
        slaveName = podTemplateConfiguration.get("slaveName", "slave")
        buildNamespace = podTemplateConfiguration.get("buildNamespace", "jenkins")
        image = podTemplateConfiguration.get("image")
        resourceRequestCpu = podTemplateConfiguration.get("resourceRequestCpu", "250m")
        resourceRequestMemory = podTemplateConfiguration.get("resourceRequestMemory", "1Gi")
        buildDaysToKeepStr = podTemplateConfiguration.get("buildDaysToKeepStr", "3")
        buildNumToKeepStr = podTemplateConfiguration.get("buildNumToKeepStr", "5")
        jobTimeoutMinutes = podTemplateConfiguration.get("jobTimeoutMinutes", "60")
        paramlist = podTemplateConfiguration.get("paramlist", []) + [booleanParam(name: 'DEBUG', description: 'Enable DEBUG mode with extended output', defaultValue: false)]
        authMap = podTemplateConfiguration.get("auth", [:])
        propertiesList = [parameters(paramlist), buildDiscarder(logRotator(daysToKeepStr: buildDaysToKeepStr, numToKeepStr: buildNumToKeepStr)), authorizationMatrix(inheritanceStrategy: nonInheriting(), permissions: securityPermissions)]
        isDisableConcurrentBuildsEnabled
    }

    buildConfig {
        language
        pathToSrc
    }

    integrationTestsConfig{

        dependenciesList
    }

    deployConfig {
        kubernetesClusterMap
        Healthcheck
        postDeployStep{mymethod()}
    }

    securityConfig{
        allowedUsersPerBranch dev s
    }

}

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





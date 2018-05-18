package com.nextiva

import static com.nextiva.SharedJobsStaticVars.getSONAR_QUBE_ENV
import static com.nextiva.SharedJobsStaticVars.getSONAR_QUBE_SCANNER


final String pathToSrc

String getVersion() {
    dir(pathToSrc) {
        def rootPom = readMavenPom file: "pom.xml"
        return rootPom.version
    }
}


void setVersion(String version) {
    dir(pathToSrc) {
        sh "mvn versions:set -DnewVersion=${version} -DgenerateBackupPoms=false"
    }
}


String createReleaseVersion(String version) {
    def releaseVersion = version.replaceAll("-SNAPSHOT", "")
    return releaseVersion
}


def runSonarScanner(String projectVersion) {
    scannerHome = tool SONAR_QUBE_SCANNER
    withSonarQubeEnv(SONAR_QUBE_ENV) {
        sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectVersion=${projectVersion}"
    }
}


void runTests(Map args) {
    print("\n\n Start unit tests Java \n\n")
    dir(pathToSrc) {
        try {
            sh 'mvn clean install jacoco:report'
            sh 'mvn checkstyle:checkstyle'
        } catch (e) {
            error("Unit test fail ${e}")
        } finally {
            junit '**/target/surefire-reports/*.xml'
            checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/target/checkstyle-result.xml', unHealthy: ''
        }
    }
}


void buildPublish(String appName, String buildVersion, String environment) {
    print("\n\n build and publish Java \n\n ")
    print("APP_NAME: ${appName} \n BUILD_VERSION: ${buildVersion} \n ENV: ${environment}")
    dir(pathToSrc) {
        try {
            sh 'mvn deploy --batch-mode -DskipTests'
        } catch (e) {
            error("buildPublish  fail ${e}")

        }
    }
}

//void setBuildVersion(String userDefinedBuildVersion) {
//    if (!userDefinedBuildVersion) {
//        version = getVersion()
//        DEPLOY_ONLY = false
//        echo('===========================')
//        echo('Source Defined Version = ' + version)
//    } else {
//        version = userDefinedBuildVersion.trim()
//        DEPLOY_ONLY = true
//        echo('===========================')
//        echo('User Defined Version = ' + version)
//    }
//
//    if (env.BRANCH_NAME ==~ /^(dev|develop)$/) {
//        BUILD_VERSION = version - "SNAPSHOT" + "-" + env.BUILD_ID
//    } else {
//        BUILD_VERSION = version
//    }
//
//    ANSIBLE_EXTRA_VARS = ['application_version': version,
//                          'maven_repo'         : version.contains('SNAPSHOT') ? 'snapshots' : 'releases']
//
//    echo('===============================')
//    echo('BUILD_VERSION ' + BUILD_VERSION)
//    echo('===============================')
//    print('DEPLOY_ONLY:  ' + DEPLOY_ONLY)
//    echo('===============================')
//}
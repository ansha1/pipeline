package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*

String getVersion(String pathToPom = '.') {
    rootPom = readMavenPom file: "${pathToPom}/pom.xml"
    return rootPom.version
}

def setVersion(String version, String pathToPom = '.') {
    sh "cd ${pathToPom} && mvn versions:set -DnewVersion=${version} -DgenerateBackupPoms=false"
}

String createReleaseVersion(String version) {
    releaseVersion = version.replaceAll("-SNAPSHOT", "")
    return releaseVersion
}

def runSonarScanner(String projectVersion) {
    scannerHome = tool SONAR_QUBE_SCANNER

    withSonarQubeEnv(SONAR_QUBE_ENV) {
        sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectVersion=${projectVersion}"
    }
}

def test(String pathToSrc = '.') {
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

def buildPublish(String pathToSrc = '.') {
    dir(pathToSrc) {
        try {
            sh 'mvn deploy --batch-mode -DskipTests'
        } catch (e) {
            error("buildPublish  fail ${e}")

        }
    }
}

def setBuildVersion(String userDefinedBuildVersion, String pathToPom = '.') {

    if (!userDefinedBuildVersion) {
        version = utils.getVersion(pathToPom)
        DEPLOY_ONLY = false
    } else {
        version = userDefinedBuildVersion.trim()
        DEPLOY_ONLY = true
        echo('===========================')
        echo('User Defined Version = ' + version)
    }

    ANSIBLE_EXTRA_VARS = ['application_version': version,
                          'maven_repo'         : version.contains('SNAPSHOT') ? 'snapshots' : 'releases']

    BUILD_VERSION = version - "SNAPSHOT" + "-" + env.BUILD_ID
    echo('===============================')
    echo('POM VERSION ' + version)
    echo('BUILD_VERSION ' + BUILD_VERSION)
    echo('===============================')
    print('DEPLOY_ONLY:  ' + DEPLOY_ONLY)
    echo('===============================')
}

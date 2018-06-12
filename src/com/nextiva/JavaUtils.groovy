package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*


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

    dir(pathToSrc) {
        withSonarQubeEnv(SONAR_QUBE_ENV) {
            sh 'mvn verify sonar:sonar'
        }
        timeout(time: 30, unit: 'MINUTES') {
            def qg = waitForQualityGate()
            if (qg.status != 'OK') {
                print('Sonar Quality Gate failed')
                // currentBuild.rawBuild.result = Result.UNSTABLE
            }
        }
    }
}


void runTests(Map args) {
    print("\n\n Start unit tests Java \n\n")
    def testCommands = args.get('testCommands', 'mvn clean install jacoco:report && mvn checkstyle:checkstyle')
    dir(pathToSrc) {
        try {
            sh testCommands
        } catch (e) {
            error("ERROR: Unit test fail ${e}")
        } finally {
            junit '**/target/surefire-reports/*.xml'
            checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/target/checkstyle-result.xml', unHealthy: ''
        }
    }
}


void buildPublish(String appName, String buildVersion, String environment, Map args) {
    print("\n\n build and publish Java \n\n ")
    print("APP_NAME: ${appName} \n BUILD_VERSION: ${buildVersion} \n ENV: ${environment}")
    def buildCommands = args.get('buildCommands', 'mvn deploy --batch-mode -DskipTests')
    dir(pathToSrc) {
        try {
            sh(returnStdout: true, script: buildCommands)
        } catch (e) {
            error("ERROR: buildPublish fail ${e}")
        }
    }
}

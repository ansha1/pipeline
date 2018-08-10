package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*
import groovy.transform.Field
import com.nextiva.MavenArtifactProperty


@Field
String pathToSrc = '.'


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
            sh 'mvn sonar:sonar'
        }
        /*timeout(time: 30, unit: 'MINUTES') {
            def qg = waitForQualityGate()
            if (qg.status != 'OK') {
                log.warning('Sonar Quality Gate failed')
                // currentBuild.rawBuild.result = Result.UNSTABLE
            }
        }*/
    }
}

def getArtifactsProperties() {
    log.info("get Java artifacts properties: groupId, version, artifactId, packaging")
    List javaObjectListProperties = []
    dir(pathToSrc) {
        def artifactsProperties = sh returnStdout: true, script: """mvn -q -Dexec.executable=\'echo\' -Dexec.args=\'\${project.groupId} \${project.version} \${project.artifactId} \${project.packaging}\' exec:exec -U"""
        log.info("artifactsProperties: ${artifactsProperties}")

        artifactsProperties.eachLine {
            def propertiesList = it.split()
            log.info("properties: ${propertiesList}")
            def myObject = new MavenArtifactProperty(groupId: propertiesList[0], artifactVersion: propertiesList[1], artifactId: propertiesList[2], packaging: propertiesList[3])
//            javaObjectListProperties << new MavenArtifactProperty(propertiesList[0], propertiesList[1], propertiesList[2], propertiesList[3])
        }
    }

    return javaObjectListProperties
}

Boolean verifyPackageInNexus(String packageName, String packageVersion, String deployEnvironment) {
    return false
}


void runTests(Map args) {
    log.info("Start unit tests Java")
    def testCommands = args.get('testCommands', 'mvn --batch-mode clean install jacoco:report && mvn checkstyle:checkstyle')
    dir(pathToSrc) {
        try {
            sh testCommands
        } catch (e) {
            error("Unit test fail ${e}")
        } finally {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/target/checkstyle-result.xml', unHealthy: ''
        }
    }
}


void buildPublish(String appName, String buildVersion, String environment, Map args) {
    log.info("Build and publish Java application.")
    log.info("APP_NAME: ${appName}")
    log.info("BUILD_VERSION: ${buildVersion}")
    log.info("ENV: ${environment}")
    def buildCommands = args.get('buildCommands', 'mvn deploy --batch-mode -DskipTests')
    dir(pathToSrc) {
        try {
            sh "${buildCommands}"
        } catch (e) {
            error("buildPublish fail ${e}")
        }
    }
}

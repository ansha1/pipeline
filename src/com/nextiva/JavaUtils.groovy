package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*
import groovy.transform.Field


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


List getArtifactsProperties() {
    log.info("get Java artifacts properties: groupId, version, artifactId, packaging")
    List artifactsListProperties = []
    dir(pathToSrc) {
        def artifactsProperties = sh returnStdout: true, script: """mvn -q -Dexec.executable=\'echo\' -Dexec.args=\'\${project.groupId} \${project.artifactId} \${project.version} \${project.packaging}\' exec:exec -U"""
        log.info("artifactsProperties: ${artifactsProperties}")

        artifactsProperties.split('\n').each {
            def propertiesList = it.split()
            log.info("properties: ${propertiesList}")
            artifactsListProperties << ['groupId': propertiesList[0], 'artifactVersion': propertiesList[2], 'artifactId': propertiesList[1], 'packaging': propertiesList[3]]
        }
    }
    log.info("method getArtifactsProperties() returned: ${artifactsListProperties}")
    return artifactsListProperties
}

Boolean isMavenArtifactVersionsEqual(List artifactsListProperties) {
    return (new HashSet(artifactsListProperties.collect { it.get('artifactVersion')}).size() == 1)
}

Boolean verifyPackageInNexus(String packageName, String packageVersion, String deployEnvironment) {
    List mavenArtifactsProperties = getArtifactsProperties()
    Integer counter = 0
    mavenArtifactsProperties.each { artifact ->
        log.info('artifact properties: ' + artifact)
        if (nexus.isJavaArtifactExists(artifact.groupId, artifact.artifactId, artifact.artifactVersion, artifact.packaging)) {
            counter++
        }
    }
    log.info("number of artifacts found: ${counter}")
    def resultOfComparison = isMavenArtifactVersionsEqual(mavenArtifactsProperties)
    log.info("is version of artifacts equal: ${resultOfComparison}")
//    if (counter.equals(0)) {
//        return false
//    } else if (counter.equals(4) && checkMavenArtifactVersion(mavenArtifactsProperties)){
//
//    }
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

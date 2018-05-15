package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*


final String pathToSrc


String getVersion() {
    dir(pathToSrc) {
        def buildProperties = readProperties file: BUILD_PROPERTIES_FILENAME
        return buildProperties.version
    }
}


void setVersion(String version) {
    dir(pathToSrc) {
        String propsToWrite = ''
        def buildProperties = readProperties file: BUILD_PROPERTIES_FILENAME
        buildProperties.version = version
        buildProperties.each {
            propsToWrite = propsToWrite + it.toString() + '\n'
        }
        writeFile file: BUILD_PROPERTIES_FILENAME, text: propsToWrite
    }
}


String createReleaseVersion(String version) {
    def releaseVersion = version.tokenize('-')[0]
    return releaseVersion
}


def runSonarScanner(String projectVersion) {
    scannerHome = tool SONAR_QUBE_SCANNER

    withSonarQubeEnv(SONAR_QUBE_ENV) {
        sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectVersion=${projectVersion}"
    }
}


void runTests() {

}


void buildPublish() {

}


void setBuildVersion(String userDefinedBuildVersion) {

}

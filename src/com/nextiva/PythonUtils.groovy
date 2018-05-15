package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*

class PythonUtils implements Utils {

    final String pathToSrc

    PythonUtils(String pathToSrc) {
        this.pathToSrc = pathToSrc
    }

    PythonUtils() {
        this.pathToSrc = '.'
    }

    @Override
    String getVersion() {
        dir(pathToSrc) {
            def buildProperties = readProperties file: BUILD_PROPERTIES_FILENAME
        }
        return buildProperties.version
    }

    @Override
    def setVersion(String version) {
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

    @Override
    String createReleaseVersion(String version) {
        def releaseVersion = version.tokenize('-')[0]
        return releaseVersion
    }

    @Override
    def runSonarScanner(String projectVersion) {
        scannerHome = tool SONAR_QUBE_SCANNER

        withSonarQubeEnv(SONAR_QUBE_ENV) {
            sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectVersion=${projectVersion}"
        }
    }

    @Override
    void runTests() {

    }

    @Override
    void buildPublish() {

    }

    @Override
    void setBuildVersion(String userDefinedBuildVersion) {

    }
}
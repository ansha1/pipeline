package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*


String getVersion(String pathToSetupPy = '.') {
    def buildProperties = readProperties file: "${pathToSetupPy}/${BUILD_PROPERTIES_FILENAME}"
    return buildProperties.version
}

def setVersion(String version, String pathToSetupPy = '.') {
    def buildProperties = readProperties file: "${pathToSetupPy}/${BUILD_PROPERTIES_FILENAME}"
    buildProperties.version = version
    writeFile file: "${pathToSetupPy}/${BUILD_PROPERTIES_FILENAME}", text: buildProperties
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

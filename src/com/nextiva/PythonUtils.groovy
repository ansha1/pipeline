package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*


String getVersion(String pathToSetupPy = '.') {
    def buildProperties = readProperties file: "${pathToSetupPy}/${BUILD_PROPERTIES_FILENAME}"
    return buildProperties.version
}

def setVersion(String version, String pathToSetupPy = '.') {
    String propsToWrite = ''
    def buildProperties = readProperties file: "${pathToSetupPy}/${BUILD_PROPERTIES_FILENAME}"

    buildProperties.version = version
    buildProperties.each {
        propsToWrite = propsToWrite + it.toString() + '\n'
    }
    writeFile file: "${pathToSetupPy}/${BUILD_PROPERTIES_FILENAME}", text: propsToWrite
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

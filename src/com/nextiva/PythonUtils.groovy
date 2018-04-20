package com.nextiva


String getVersion(String pathToSetupPy='.'){
    def buildProperties = readProperties  file: "${pathToSetupPy}/${BUILD_PROPERTIES_FILENAME}"
    return buildProperties.version
}

def setVersion(String version, String pathToSetupPy='.'){
    def buildProperties = readProperties  file: "${pathToSetupPy}/${BUILD_PROPERTIES_FILENAME}"
    buildProperties.version = version
    writeFile file: "${pathToSetupPy}/${BUILD_PROPERTIES_FILENAME}", text: buildProperties
}

String createReleaseVersion(String version){
    def releaseVersion = version.tokenize('-')[0]
    return releaseVersion
}

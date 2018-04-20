package com.nextiva

String getVersion(String pathToPom='.'){
    rootPom = readMavenPom file: "${pathToPom}/pom.xml"
    return rootPom.version
}

def setVersion(String version, String pathToPom='.'){
    sh "cd ${pathToPom} && mvn versions:set -DnewVersion=${version} -DgenerateBackupPoms=false"
}

String createReleaseVersion(String version){
    releaseVersion = version.replaceAll("-SNAPSHOT", "")
    return releaseVersion
}

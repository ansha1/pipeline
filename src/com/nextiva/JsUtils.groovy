package com.nextiva
import static com.nextiva.SharedJobsStaticVars.*


String getVersion(String pathToPackageJs='.'){

    return version
}

def setVersion(String version, String pathToPackageJs='.'){

}

String createReleaseVersion(String version){

    return releaseVersion
}

def runSonarScanner(String projectVersion){
    scannerHome = tool SONAR_QUBE_SCANNER

    withSonarQubeEnv(SONAR_QUBE_ENV) {
        sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectVersion=${projectVersion}"
    }
}

package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*


String getVersion(String pathToPackageJs = '.') {
    packageJson = readJSON file: "${pathToPackageJs}/package.json"
    return packageJson.version

}

def setVersion(String version, String pathToPackageJs = '.') {
    packageJson = readJSON file: "${pathToPackageJs}/package.json"
    packageLockJson = readJSON file: "${pathToPackageJs}/package-lock.json"

    packageJson.version = version
    packageLockJson.version = version

    writeJSON file: "${pathToPackageJs}/package.json", json: packageJson, pretty: 1
    writeJSON file: "${pathToPackageJs}/package-lock.json", json: packageLockJson, pretty: 1

    print("\n Seted version: ${packageJson.version}\n")
}

String createReleaseVersion(String version) {

    return version
}

def runSonarScanner(String projectVersion) {
    scannerHome = tool SONAR_QUBE_SCANNER

    withSonarQubeEnv(SONAR_QUBE_ENV) {
        sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectVersion=${projectVersion}"
    }
}

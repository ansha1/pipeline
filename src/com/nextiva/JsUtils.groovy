package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*

class JsUtils implements Utils {

    final String pathToSrc

    JsUtils(String pathToSrc) {
        this.pathToSrc = pathToSrc
    }

    JsUtils() {
        this.pathToSrc = '.'
    }

    String getVersion() {
        dir(pathToSrc) {
            def packageJson = readJSON file: "package.json"
        }
        return packageJson.version
    }

    def setVersion(String version) {
        dir(pathToSrc) {
            def packageJson = readJSON file: "package.json"
            def packageLockJson = readJSON file: "package-lock.json"

            packageJson.version = version
            packageLockJson.version = version

            writeJSON file: "package.json", json: packageJson, pretty: 1
            writeJSON file: "package-lock.json", json: packageLockJson, pretty: 1

            print("\n Seted version: ${packageJson.version}\n")
        }
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
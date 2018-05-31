package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*


final String pathToSrc


String getVersion() {
    dir(pathToSrc) {
        def packageJson = readJSON file: "package.json"
        return packageJson.version
    }
}


void setVersion(String version) {
    dir(pathToSrc) {
        def packageJson = readJSON file: "package.json"
        def packageLockJson = readJSON file: "package-lock.json"

        packageJson.version = version
        packageLockJson.version = version

        writeJSON file: "package.json", json: packageJson, pretty: 1
        writeJSON file: "package-lock.json", json: packageLockJson, pretty: 1

        print("\n Set version: ${packageJson.version}\n")
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


void runTests(Map args) {
    try {
        print("\n\n Start unit tests Js \n\n")
        def languageVersion = args.get('languageVersion')
        def testCommands = args.get('testCommands', 'npm install && npm run test && npm run lint')

        dir(pathToSrc) {
            sh testCommands
        }
    } catch (e) {
        error("ERROR: Unit test fail ${e}")
    } finally {
        publishHTML([allowMissing         : true,
                     alwaysLinkToLastBuild: false,
                     keepAll              : false,
                     reportDir            : pathToSrc,
                     reportFiles          : 'test-report.html',
                     reportName           : 'Test Report',
                     reportTitles         : ''])
    }
}


void buildPublish(String appName, String buildVersion, String environment, Map args) {
    print("\n\n build and publish Js \n\n ")
    print("APP_NAME: ${appName} \n BUILD_VERSION: ${buildVersion} \n ENV: ${environment}")
    def distPath = args.get('distPath', 'dist/static')
    def buildCommands = args.get('buildCommands', "export OUTPUT_PATH=${distPath} && npm install && npm run dist")

    dir(pathToSrc) {
        sh(returnStdout: true, script: buildCommands)
        archiveToNexus(environment, 'dist/static', buildVersion, appName)
    }
}

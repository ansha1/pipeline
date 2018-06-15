package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*
import groovy.transform.Field


@Field
String pathToSrc = '.'


String getVersion() {
    dir(pathToSrc) {
        def packageJson = readJSON file: "package.json"
        return packageJson.version
    }
}


void setVersion(String version) {
    dir(pathToSrc) {
        print("\n Set version: ${version}\n")
        sh "npm version ${version} --no-git-tag-version"
    }
}


String createReleaseVersion(String version) {
    return version
}


def runSonarScanner(String projectVersion) {
    dir(pathToSrc) {
        sonarScanner(projectVersion)
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
        archiveToNexus(environment, distPath, buildVersion, appName)
    }
}

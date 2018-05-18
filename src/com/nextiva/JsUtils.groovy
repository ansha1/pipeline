package com.nextiva

import static com.nextiva.SharedJobsStaticVars.getSONAR_QUBE_ENV
import static com.nextiva.SharedJobsStaticVars.getSONAR_QUBE_SCANNER


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


void runTests(Map args) {
    try {
        print("\n\n Start unit tests Js \n\n")
        def languageVersion = args.get('languageVersion')
        def testArgs = args.get('testCommands', 'npm install && npm run test && npm run lint')

        dir(pathToSrc) {
            sh(returnStdout: true, script: testArgs)
        }
    } catch (e) {
        error("Unit test fail ${e}")
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


void buildPublish(String appName, String buildVersion, String environment) {
    print("\n\n build and publish Js \n\n ")
    print("APP_NAME: ${appName} \n BUILD_VERSION: ${buildVersion} \n ENV: ${environment}")

    dir(pathToSrc) {
        sh """
            export OUTPUT_PATH=dist/static
            npm install
            npm run dist
            """
        archiveToNexus(environment, 'dist/static', buildVersion, appName)
    }
}

//
//void setBuildVersion(String userDefinedBuildVersion) {
//
//    if (!userDefinedBuildVersion) {
//        version = getVersion()
//        DEPLOY_ONLY = false
//        echo('===========================')
//        echo('Source Defined Version = ' + version)
//    } else {
//        version = userDefinedBuildVersion.trim()
//        DEPLOY_ONLY = true
//        echo('===========================')
//        echo('User Defined Version = ' + version)
//    }
//
//    if (env.BRANCH_NAME ==~ /^(dev|develop)$/) {
//        BUILD_VERSION = version + "-" + env.BUILD_ID
//    } else {
//        BUILD_VERSION = version
//    }
//
//    ANSIBLE_EXTRA_VARS = ['version'            : BUILD_VERSION,
//                          'component_name'     : jobConfig.APP_NAME,
//                          'static_assets_files': jobConfig.APP_NAME]
//
//    echo('===============================')
//    echo('BUILD_VERSION ' + BUILD_VERSION)
//    echo('===============================')
//    print('DEPLOY_ONLY: ' + DEPLOY_ONLY)
//    echo('===============================')
//}

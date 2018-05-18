package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*


final String pathToSrc


String getVersion() {
    dir(pathToSrc) {
        def buildProperties = readProperties file: BUILD_PROPERTIES_FILENAME
        return buildProperties.version
    }
}


void setVersion(String version) {
    dir(pathToSrc) {
        String propsToWrite = ''
        def buildProperties = readProperties file: BUILD_PROPERTIES_FILENAME
        buildProperties.version = version
        buildProperties.each {
            propsToWrite = propsToWrite + it.toString() + '\n'
        }
        writeFile file: BUILD_PROPERTIES_FILENAME, text: propsToWrite
    }
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


void runTests(Map args) {
    //TODO: add publish test report step
    try {
        print("\n\n Start unit tests Python \n\n")
        def languageVersion = args.get('languageVersion', 'python3.6')
        def testArgs = args.get('testCmds')

        dir(pathToSrc) {
            pythonUtils.createVirtualEnv(languageVersion)
            pythonUtils.venvSh(testArgs)
        }
    } catch (e) {
        error("Unit test fail ${e}")
    }
}


void buildPublish(String appName, String buildVersion, String environment) {
    print("\n\n build and publish Python \n\n ")
    print("APP_NAME: ${appName} \n BUILD_VERSION: ${buildVersion} \n ENV: ${environment}")

    debPackage.build(appName, buildVersion, environment, pathToSrc)
    debPackage.publish(appName, environment, pathToSrc)
}

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
//    ANSIBLE_EXTRA_VARS = ['version': BUILD_VERSION]
//
//    echo('===============================')
//    echo('BUILD_VERSION ' + BUILD_VERSION)
//    echo('===============================')
//    print('DEPLOY_ONLY: ' + DEPLOY_ONLY)
//    echo('===============================')
//}

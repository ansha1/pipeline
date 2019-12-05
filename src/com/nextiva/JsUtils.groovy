package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*
import groovy.transform.Field


@Field String pathToSrc = '.'
@Field String modulesPropertiesField = ''

String getVersion() {
    dir(pathToSrc) {
        def packageJson = readJSON file: "package.json"
        return packageJson.version
    }
}


void setVersion(String version) {
    dir(pathToSrc) {
        log.info("Set version: ${version}")
        sh "npm version ${version} --no-git-tag-version --allow-same-version"
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


Boolean verifyPackageInNexus(String packageName, String packageVersion, String deployEnvironment) {
    nexus.isAssetsPackageExists(packageName, packageVersion)
}


void runTests(Map projectFlow) {
    try {
        log.info("Start unit tests JavaScript")
        def testCommands = projectFlow.get('testCommands', 'npm install && npm run test && npm run lint')

        dir(pathToSrc) {
            sh testCommands
        }
    } catch (e) {
        error("Unit test fail ${e}")
    }
}

def buildAssets(Map projectFlow) {
    def distPath = projectFlow.get('distPath', 'dist/static')
    def buildCommands = projectFlow.get('buildCommands', "export OUTPUT_PATH=${distPath} && npm install && npm run dist")
    String staticAssetsAddress = projectFlow.get('staticAssetsAddress')

    dir(pathToSrc) {
        withEnv(["STATIC_ASSETS_ADDRESS=${staticAssetsAddress}"]) {
            sh "${buildCommands}"
        }
    }
}

def publishAssets(String appName, String buildVersion, String environment, Map projectFlow) {
    def distPath = projectFlow.get('distPath', 'dist/static')
    Boolean publishToS3 = projectFlow.get('publishStaticAssetsToS3', PUBLISH_STATIC_ASSETS_TO_S3_DEFAULT)
    log.info("publishStaticAssetsToS3: ${publishToS3}")

    dir(pathToSrc) {
        nexus.uploadStaticAssets(environment, distPath, buildVersion, appName, pathToSrc)

        if (publishToS3) {
            aws.uploadFrontToS3(appName, buildVersion, environment, projectFlow, pathToSrc)
        }    
    }
}

void buildPublish(String appName, String buildVersion, String environment, Map projectFlow) {
    log.info("Build and publish JavaScript application.")
    log.info("APP_NAME: ${appName}")
    log.info("BUILD_VERSION: ${buildVersion}")
    log.info("ENV: ${environment}")
    buildAssets(projectFlow)
    publishAssets(appName, buildVersion, environment, projectFlow)
}

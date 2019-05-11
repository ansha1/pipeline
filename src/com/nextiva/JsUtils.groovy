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


void runTests(Map args) {
    try {
        log.info("Start unit tests JavaScript")
        def languageVersion = args.get('languageVersion')
        def testCommands = args.get('testCommands', 'npm install && npm run test && npm run lint')

        dir(pathToSrc) {
            sh testCommands
        }
    } catch (e) {
        error("Unit test fail ${e}")
    }
}

def buildAssets(Map args) {
    def distPath = args.get('distPath', 'dist/static')
    def buildCommands = args.get('buildCommands', "export OUTPUT_PATH=${distPath} && npm install && npm run dist")
    dir(pathToSrc) {
        sh "${buildCommands}"
    }
}

def publishAssets(String appName, String buildVersion, String environment, Map args) {
    def distPath = args.get('distPath', 'dist/static')
    Boolean publishToS3 = args.get('publishStaticAssetsToS3')
    log.info("publishStaticAssetsToS3: ${publishToS3}")

    dir(pathToSrc) {
        nexus.uploadStaticAssets(environment, distPath, buildVersion, appName, pathToSrc)
        if (publishToS3) {
            aws.uploadFrontToS3(appName, buildVersion, environment, args, pathToSrc)
        }    
    }
}

void buildPublish(String appName, String buildVersion, String environment, Map args) {
    log.info("Build and publish JavaScript application.")
    log.info("APP_NAME: ${appName}")
    log.info("BUILD_VERSION: ${buildVersion}")
    log.info("ENV: ${environment}")
    buildAssets(args)
    publishAssets(appName, buildVersion, environment, args)
}

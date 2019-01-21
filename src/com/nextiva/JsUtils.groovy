package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*
import groovy.transform.Field


@Field String pathToSrc = '.'
@Field String modulesPropertiesField = ''
@Field String PUBLISH_STATIC_ASSETS_TO_S3 = ''


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

def buildAssets(String appName, String buildVersion, String environment, Map args) {
    def pathToSrc = '.'
    def jobName = env.JOB_NAME
    def distPath = args.get('distPath', 'dist/static')
    def pathToBuildPropertiesFile = "${env.WORKSPACE}/${pathToSrc}/${BUILD_PROPERTIES_FILENAME}"
    def buildCommands = args.get('buildCommands', "export OUTPUT_PATH=${distPath} && npm install && npm run dist")
    if (environment in LIST_OF_ENVS) {
            dir(pathToSrc) {
                generateBuildProperties(environment, buildVersion, jobName)
                sh "${buildCommands}"
            }    
        } else {
                throw new IllegalArgumentException("Provided env ${environment} is not in the list ${LIST_OF_ENVS}")
    }
}

def publishAssets(String appName, String buildVersion, String environment, Map args) {
    def distPath = args.get('distPath', 'dist/static')
    def pathToSrc = '.'
    def S3BucketName = ""
    dir(pathToSrc) {
        nexus.uploadStaticAssets(environment, distPath, buildVersion, appName, pathToSrc)
        aws.uploadFrontToS3(appName, buildVersion, environment, args, pathToSrc)
    }
}

void buildPublish(String appName, String buildVersion, String environment, Map args) {
    log.info("Build and publish JavaScript application.")
    log.info("APP_NAME: ${appName}")
    log.info("BUILD_VERSION: ${buildVersion}")
    log.info("ENV: ${environment}")
    buildAssets(appName, buildVersion, environment, args)
    publishAssets(appName, buildVersion, environment, args)

}

package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*
import groovy.transform.Field


@Field
String pathToSrc = '.'


String getVersion() {
    dir(pathToSrc) {
        if ( fileExists(BUILD_PROPERTIES_FILENAME) ) {
            def buildProperties = readProperties file: BUILD_PROPERTIES_FILENAME
            if ( buildProperties.version ){
                return buildProperties.version
            }
            else {
                currentBuild.rawBuild.result = Result.ABORTED
                throw new hudson.AbortException("ERROR: Version is not specified in ${BUILD_PROPERTIES_FILENAME}.")
            }
        }
        else {
            currentBuild.rawBuild.result = Result.ABORTED
            throw new hudson.AbortException("ERROR: File ${BUILD_PROPERTIES_FILENAME} not found.")
        }
    }
}


void setVersion(String version) {
    dir(pathToSrc) {
        println("going to set a version ${version} within path ${pathToSrc}")
        String propsToWrite = ''
        def buildProperties = readProperties file: BUILD_PROPERTIES_FILENAME
        println("buildProperties: ${buildProperties}")
        buildProperties.version = version
        buildProperties.each {
            propsToWrite = propsToWrite + it.toString() + '\n'
            println("propsToWrite: ${propsToWrite}")
        }
        writeFile file: BUILD_PROPERTIES_FILENAME, text: propsToWrite
    }
}


String createReleaseVersion(String version) {
    def releaseVersion = version.tokenize('-')[0]
    return releaseVersion
}


def runSonarScanner(String projectVersion) {
    dir(pathToSrc) {
        sonarScanner(projectVersion)
    }
}


void runTests(Map args) {
    //TODO: add publish test report step

    log.info('============================')
    log.info('Start Python unit tests')
    log.info('============================')
    
    def languageVersion = args.get('languageVersion', 'python3.6')
    def testCommands = args.get('testCommands', '''pip install -r requirements.txt
                                                       pip install -r requirements-test.txt
                                                       python setup.py test''')
    def testPostCommands = args.get('testPostCommands')

    dir(pathToSrc) {
        pythonUtils.createVirtualEnv(languageVersion)
        try {
            pythonUtils.venvSh(testCommands)
        } catch (e) {
            error("Unit test fail ${e}")
        } finally {
            if(testPostCommands) {
                log.info('============================')
                log.info('Starting a cleanup after unit tests execution')
                log.info('============================')
                
                pythonUtils.venvSh(testPostCommands)
            }
        }
    }
}


void buildPublish(String appName, String buildVersion, String environment, Map args) {
    log.info("Build and publish Python.")
    log.info("APP_NAME: ${appName}")
    log.info("BUILD_VERSION: ${buildVersion}")
    log.info("ENV: ${environment}")

    debPackage.build(appName, buildVersion, environment, pathToSrc)
    debPackage.publish(appName, environment, pathToSrc)
}

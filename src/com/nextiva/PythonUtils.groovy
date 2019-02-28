package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*
import groovy.transform.Field


@Field String pathToSrc = '.'
@Field String modulesPropertiesField = ''


String getVersion() {
    dir(pathToSrc) {
        if ( fileExists(BUILD_PROPERTIES_FILENAME) ) {
            def buildProperties = readProperties file: BUILD_PROPERTIES_FILENAME
            if ( buildProperties.version ){
                return buildProperties.version
            }
            else {
                currentBuild.rawBuild.result = Result.ABORTED
                throw new hudson.AbortException("Version is not specified in ${BUILD_PROPERTIES_FILENAME}.")
            }
        }
        else {
            currentBuild.rawBuild.result = Result.ABORTED
            throw new hudson.AbortException("File ${BUILD_PROPERTIES_FILENAME} not found.")
        }
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
    dir(pathToSrc) {
        sonarScanner(projectVersion)
    }
}


Boolean verifyPackageInNexus(String packageName, String packageVersion, String deployEnvironment) {
    nexus.isDebPackageExists(packageName, packageVersion, deployEnvironment)
}

/ *
 
 source clear scans from python are different, if you do not set up the virtual environment the scan will fail, 
 not sure how to make that work in a vars class.

*/

void runSourceClearScanner(String languageVersion) {
    log.info('============================')
    log.info('Start source clear scan')
    log.info('============================')
    dir(pathToSrc) {
        def sourceClearCi = libraryResource 'sourceclear/sc.sh'
        log.info("sourceClearCi: ${sourceClearCi}")
        pythonUtils.createVirtualEnv(languageVersion)
        try {

            withEnv(["SRCCLR_CI_JSON=1", "DEBUG=1", "NOCACHE=1"]) {
                pythonUtils.venvSh """${sourceClearCi}"""
            }
        } catch (e) {
            error("Sourceclear scan fail ${e}")
        }

    }
}


void runTests(Map args) {
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
            try {
                step([$class: 'WarningsPublisher',
                      canComputeNew: false,
                      canResolveRelativePaths: false,
                      consoleParsers: [[parserName: 'ESLint'], [parserName: 'Flake8'], [parserName: 'Stylelint']],
                      defaultEncoding: '',
                      excludePattern: '',
                      healthy: '',
                      includePattern: '',
                      messagesPattern: '',
                      unHealthy: ''])
                step([$class: 'AnalysisPublisher',
                      canComputeNew: false,
                      checkStyleActivated: false,
                      defaultEncoding: '',
                      findBugsActivated: false,
                      healthy: '',
                      unHealthy: ''])

                junit allowEmptyResults: true, testResults: '**/junit.xml'

                step([$class: 'CoberturaPublisher', 
                      autoUpdateHealth: false, 
                      autoUpdateStability: false, 
                      coberturaReportFile: '**/coverage.xml', 
                      failUnhealthy: false, 
                      failUnstable: false, 
                      maxNumberOfBuilds: 0, 
                      onlyStable: false, 
                      sourceEncoding: 'ASCII', 
                      zoomCoverageChart: false])

                allure includeProperties: false, jdk: '', results: [[path: 'allure-results']]
            } catch (e) {
                log.warning("there was a problem with test coverage pubish step: ${e}")
            }

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

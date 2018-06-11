package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*


final String pathToSrc


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

    dir(pathToSrc) {
        withSonarQubeEnv(SONAR_QUBE_ENV) {
            sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectVersion=${projectVersion}"
        }
    }
}


void runTests(Map args) {
    //TODO: add publish test report step

    println('============================')
    println("Start Python unit tests")
    println('============================')
    
    def languageVersion = args.get('languageVersion', 'python3.6')
    def testCommands = args.get('testCommands', '''pip install -r requirements.txt
                                                       pip install -r requirements-test.txt
                                                       python setup.py test''')
    def testPostCommands = args.get('testPostCommands')

    try {
        dir(pathToSrc) {
            pythonUtils.createVirtualEnv(languageVersion)
            pythonUtils.venvSh(testCommands)
        }
    } catch (e) {
        error("Unit test fail ${e}")
    } finally {
        println('============================')
        println("Starting a cleanup after unit tests execution")
        println('============================')
        
        dir(pathToSrc) {
            pythonUtils.venvSh(testPostCommands)
        }
    }
}


void buildPublish(String appName, String buildVersion, String environment, Map args) {
    print("\n\n build and publish Python \n\n ")
    print("APP_NAME: ${appName} \n BUILD_VERSION: ${buildVersion} \n ENV: ${environment}")

    debPackage.build(appName, buildVersion, environment, pathToSrc)
    debPackage.publish(appName, environment, pathToSrc)
}

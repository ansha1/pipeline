package com.nextiva.tools.build

import hudson.AbortException

import static com.nextiva.SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
import static com.nextiva.utils.Utils.getPropertyFromFile

class Pip extends BuildTool {

    def unitTestCommands = """
                           pip install -r requirements.txt
                           pip install -r requirements-test.txt
                           python setup.py test
                           """
    def publishCommands = {
        script.buildPublishPypiPackage(pathToSrc)
    }

    Pip(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
    }

    @Override
    Boolean setVersion(String version) {
        execute {
            log.debug("Set this version:$version as GLOBAL_VERSION")
            script.GLOBAL_VERSION = version
            String propsToWrite = ''
            def buildProperties = script.readProperties file: BUILD_PROPERTIES_FILENAME
            buildProperties.version = version
            buildProperties.each {
                propsToWrite = propsToWrite + it.toString() + '\n'
            }
            script.writeFile file: BUILD_PROPERTIES_FILENAME, text: propsToWrite
            return true
        }
    }

    @Override
    String getVersion() {
        execute {
            String version = getPropertyFromFile(script, BUILD_PROPERTIES_FILENAME, "version")
            if (version == null) {
                throw new AbortException("Version is not specified in ${BUILD_PROPERTIES_FILENAME}.")
            }
            if (script.GLOBAL_VERSION == null) {
                log.debug("Set this version:$version as GLOBAL_VERSION")
                script.GLOBAL_VERSION = version
            }
            return version
        }
    }

    @Override
    void sonarScan() {
        execute {
            //TODO: add sonar implementation
            log.info("this step should be implemented")
        }
    }

    @Override
    void securityScan() {
        execute {
            //TODO: add securityScan implementation
            log.info("this step should be implemented")
        }
    }

    @Override
    Boolean isArtifactAvailableInRepo() {
        execute {
            return script.nexus.isPypiPackageExists(appName, getVersion(), "pypi-production")
        }
    }
}

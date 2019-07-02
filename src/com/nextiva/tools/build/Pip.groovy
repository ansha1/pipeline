package com.nextiva.tools.build

import hudson.AbortException

import static com.nextiva.SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
import static com.nextiva.utils.Utils.getPropertyFromFile
import static com.nextiva.utils.Utils.setPropertyToFile

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
    void setVersion(String version) {
        execute {
            setPropertyToFile(script, BUILD_PROPERTIES_FILENAME, "version", version)
        }
    }

    @Override
    String getVersion() {
        execute {
            String version = getPropertyFromFile(script, BUILD_PROPERTIES_FILENAME, "version")
            if (version == null) {
                throw new AbortException("Version is not specified in ${BUILD_PROPERTIES_FILENAME}.")
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

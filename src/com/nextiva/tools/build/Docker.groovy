package com.nextiva.tools.build

import hudson.AbortException

import static com.nextiva.SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
import static com.nextiva.utils.Utils.getPropertyFromFile

class Docker extends BuildTool {

    def publishCommands = {
        //some publish commands
    }

    Docker(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
    }

    @Override
    String getVersion() {
        execute {
            String version = null
            try {
                version = getPropertyFromFile(script, BUILD_PROPERTIES_FILENAME, "version")
            } catch (e){
                log.debug("$BUILD_PROPERTIES_FILENAME not found for the Docker build tool, e:", e)
            }
            if (version == null) {
                log.debug("Try to get version from GLOBAL version")
                version = script.GLOBAL_VERSION
            }
            if (version == null) {
                throw new AbortException("Version for Docker is undefined, please define it in $BUILD_PROPERTIES_FILENAME or by another build tool via GLOBAL version")
            }
        }
    }

    @Override
    Boolean setVersion(String version) {
        execute {
            String propsToWrite = ''
            if (script.fileExists(BUILD_PROPERTIES_FILENAME)) {
                def buildProperties = script.readProperties file: BUILD_PROPERTIES_FILENAME
                buildProperties.version = version
                buildProperties.each {
                    propsToWrite = propsToWrite + it.toString() + '\n'
                }
                script.writeFile file: BUILD_PROPERTIES_FILENAME, text: propsToWrite
            } else {
                log.warn("File ${BUILD_PROPERTIES_FILENAME} not found, can not set version")
            }
            return true
        }
    }

    @Override
    void sonarScan() {
        log.debug("sonarScan is not exist for this type of Build tool")
    }

    @Override
    void securityScan() {
        //TODO: we should implement security scan for docker containers
        log.warn("we should implement security scan for docker containers")
    }

    @Override
    Boolean isArtifactAvailableInRepo() {
        execute {
            log.debug("try to find the docker image ${appName} with version:${getVersion()} in the Nexus registry")
            return script.nexus.isDockerPackageExists(appName, getVersion())
        }
    }
}

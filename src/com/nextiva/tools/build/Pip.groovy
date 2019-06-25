package com.nextiva.tools.build

import hudson.AbortException

import static com.nextiva.SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME

class Pip extends BuildTool {

    Pip(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
    }

    @Override
    Boolean setVersion(String version) {
        String propsToWrite = ''
        def buildProperties = script.readProperties file: BUILD_PROPERTIES_FILENAME
        buildProperties.version = version
        buildProperties.each {
            propsToWrite = propsToWrite + it.toString() + '\n'
        }
        script.writeFile file: BUILD_PROPERTIES_FILENAME, text: propsToWrite
        return true
    }

    @Override
    String getVersion() {
        if (script.fileExists(BUILD_PROPERTIES_FILENAME)) {
            def buildProperties = script.readProperties file: BUILD_PROPERTIES_FILENAME
            if (buildProperties.version) {
                return buildProperties.version
            } else {
                throw new AbortException("Version is not specified in ${BUILD_PROPERTIES_FILENAME}.")
            }
        } else {
            throw new AbortException("File ${BUILD_PROPERTIES_FILENAME} not found.")
        }
    }

    @Override
    void sonarScan() {

    }

    @Override
    void securityScan() {

    }

    @Override
    void publish() {

    }

    @Override
    Boolean isArtifactAvailableInRepo() {
        return null
    }
}

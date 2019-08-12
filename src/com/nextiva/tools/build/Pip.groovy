package com.nextiva.tools.build

import hudson.AbortException

import static com.nextiva.SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
import static com.nextiva.utils.Utils.getPropertyFromFile
import static com.nextiva.utils.Utils.setPropertyToFile
import static com.nextiva.utils.Utils.shWithOutput

class Pip extends BuildTool {

    def defaultCommands = [
            unitTest: """\
                pip install -r requirements.txt
                pip install -r requirements-test.txt
                python setup.py test
            """.stripIndent(),
            publish : {
                script.container(name) {
                    def command = 'pip install twine'
                    def output = shWithOutput(script, command)
                    logger.info("$output")
                    return output
                }
                script.buildPublishPypiPackage(pathToSrc, null, 'python')
            },
            build   : 'pip install -r requirements.txt'
    ]

    Pip(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
        if (unitTestCommands == null) {
            unitTestCommands = defaultCommands.unitTest
        }
        if (publishCommands == null) {
            publishCommands = defaultCommands.publish
        }
        if (buildCommands == null) {
            buildCommands = defaultCommands.build
        }
    }

    @Override
    void setVersion(String version) {
        execute {
            setPropertyToFile(script, BUILD_PROPERTIES_FILENAME, "version", version)
        }
    }

    @Override
    String getVersion() {
        return execute {
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
            logger.info("this step should be implemented")
        }
    }

    @Override
    void securityScan() {
        execute {
            //TODO: add securityScan implementation
            logger.info("this step should be implemented")
        }
    }

    @Override
    Boolean isArtifactAvailableInRepo() {
        execute {
            return script.nexus.isPypiPackageExists(appName, getVersion(), "pypi-production")
        }
    }
}

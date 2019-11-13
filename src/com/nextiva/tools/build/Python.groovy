package com.nextiva.tools.build

import com.cloudbees.groovy.cps.NonCPS
import com.nextiva.config.DeploymentType
import hudson.AbortException

import static com.nextiva.SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
import static com.nextiva.config.Config.instance as config
import static com.nextiva.utils.Utils.getPropertyFromFile
import static com.nextiva.utils.Utils.setPropertyToFile
import static com.nextiva.utils.Utils.shWithOutput

class Python extends BuildTool {

    String pipIndex

    def defaultCommands = [
            unitTest: """\
                export PIP_INDEX="$pipIndex"
                pip install -r requirements.txt
                pip install -r requirements-test.txt
                python setup.py test
            """.stripIndent(),
            publish : {
                config.script.container(name) {
                    def command = 'pip install twine'
                    def output = shWithOutput(config.script, command)
                    logger.info("$output")
                    return output
                }
                config.script.buildPublishPypiPackage(pathToSrc, null, 'python')
            },
            build   : 'pip install -r requirements.txt'
    ]

    Python(Map toolConfiguration) {
        super(toolConfiguration)

        this.pipIndex = toolConfiguration.pipIndex

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
            setPropertyToFile(config.script, BUILD_PROPERTIES_FILENAME, "version", version)
        }
    }

    @Override
    String getVersion() {
        return execute {
            String version = getPropertyFromFile(config.script, BUILD_PROPERTIES_FILENAME, "version")
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
            return config.script.nexus.isPypiPackageExists(appName, getVersion(), "pypi-production")
        }
    }
}

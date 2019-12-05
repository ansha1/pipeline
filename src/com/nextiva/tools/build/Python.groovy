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
            unitTest: '''\
                pip install -r requirements.txt
                pip install -r requirements-test.txt
                python setup.py test
            '''.stripIndent(),
            publish : {
                config.script.container(name) {
                    logger.trace("Installing twine")
                    def output = config.script.sh(script: 'pip install twine 2>&1', returnStdout: true)
                    logger.trace("Twine installed")
                    logger.info("$output")
                }
                config.script.buildPublishPypiPackage(pathToSrc, null, 'python')
            },
            build   : """pip install -U wheel
                         python setup.py sdist bdist bdist_egg bdist_wheel"""
    ]

    Python(Map toolConfiguration) {
        super(toolConfiguration)

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

    @Override
    void publish() {
        config.script.stage("python: publishArtifact") {
            config.script.container(name) {
                logger.trace("Installing twine")
                def output = config.script.sh(script: 'pip install twine 2>&1', returnStdout: true)
                logger.trace("Twine installed")
                logger.info("$output")
                config.script.withCredentials([config.script.usernamePassword(
                        credentialsId: '13901a38-4279-4ee5-bfe6-f33e41d0a1ee',
                        usernameVariable: 'TWINE_USERNAME',
                        passwordVariable: 'TWINE_PASSWORD')
                ]) {
                    output = config.script.sh(script: "twine upload dist/* 2>&1", returnStdout: true)
                }
            }
        }
    }
}

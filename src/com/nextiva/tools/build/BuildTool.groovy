package com.nextiva.tools.build

import com.nextiva.tools.Tool
import com.nextiva.utils.Logger

import static com.nextiva.config.Config.instance as config
import static com.nextiva.utils.Utils.shOrClosure

abstract class BuildTool implements Serializable, Tool {

    String name
    String appName
    Logger logger

    String pathToSrc = "."
    def buildCommands
    def postBuildCommands
    def unitTestCommands
    def postUnitTestCommands
    def integrationTestCommands
    def postIntegrationTestCommands
    def postDeployCommands
    def publishCommands
    Boolean publishArtifact = true

    BuildTool(Map buildToolConfig) {
        //Try to get sh command or closure from configuration and replace in with default commands if exist
        this.name = buildToolConfig.get("name") ?: this.name
        this.pathToSrc = buildToolConfig.get("pathToSrc") ?: this.pathToSrc
        this.buildCommands = buildToolConfig.get("buildCommands") ?: this.buildCommands
        this.postBuildCommands = buildToolConfig.get("postBuildCommands") ?: this.postBuildCommands
        this.unitTestCommands = buildToolConfig.get("unitTestCommands") ?: this.unitTestCommands
        this.postUnitTestCommands = buildToolConfig.get("postUnitTestCommands") ?: this.postUnitTestCommands
        this.integrationTestCommands = buildToolConfig.get("integrationTestCommands") ?: this.integrationTestCommands
        this.postIntegrationTestCommands = buildToolConfig.get("postIntegrationTestCommands") ?: this.postIntegrationTestCommands
        this.postDeployCommands = buildToolConfig.get("postDeployCommands") ?: this.postDeployCommands
        this.publishCommands = buildToolConfig.get("publishCommands") ?: this.publishCommands
        if (buildToolConfig.get("publishArtifact") != null) {
            this.publishArtifact = buildToolConfig.get("publishArtifact")
        }
        this.logger = new Logger(this)

    }

    abstract String getVersion()

    abstract void setVersion(String version)

    abstract void sonarScan()

    abstract void securityScan()

    abstract Boolean isArtifactAvailableInRepo()

    void build() {
        tryExec("build", buildCommands, postBuildCommands)
    }

    void unitTest() {
        tryExec("unitTest", unitTestCommands, postUnitTestCommands)
    }

    void integrationTest() {
        tryExec("integrationTest", integrationTestCommands, postIntegrationTestCommands)
    }

    void postDeploy() {
        tryExec("postDeploy", postDeployCommands, null)
    }

    void publish() {
        if (publishArtifact == false) {
            logger.info("Skipping publish, because publishArtifact is set to false")
            return
        }
        tryExec("publishArtifact", publishCommands, null)
    }

    def execute(def command) {
        config.script.dir(pathToSrc) {
            logger.debug("executing command in container ${name}")
            config.script.container(name) {
                def output = shOrClosure(config.script, command)
                logger.info("Command output: $output")
                return output
            }
        }
    }

    def tryExec(String action, def commands, def postCommands) {
        if (commands != null) {
            config.script.stage("${name}: ${action}") {
                try {
                    logger.debug("Executing ", commands)
                    execute(commands)
                } catch (e) {
                    logger.error("Error when executing ${name} ${action}: ${commands}", e)
                    throw e
                } finally {
                    if (postCommands != null) {
                        try {
                            logger.debug("Executing post commands", postCommands)
                            execute(postCommands)
                        } catch (e) {
                            logger.error("Error when executing ${name} ${action}: ${postCommands}", e)
                            throw e
                        }
                    }
                }
            }
        }
    }
}
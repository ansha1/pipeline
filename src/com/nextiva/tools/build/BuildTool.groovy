package com.nextiva.tools.build

import com.nextiva.tools.Tool
import com.nextiva.utils.Logger

import static com.nextiva.utils.Utils.shOrClosure

abstract class BuildTool implements Serializable, Tool {

    Script script
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
    Boolean publishArtifact = false

    BuildTool(Script script, Map buildToolConfig) {
        this.script = script
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
        tryExec("publishArtifact", publishCommands, null)
    }

    def execute(def command) {
        script.dir(pathToSrc) {
            logger.debug("executing command in container ${name}")
            script.container(name) {
                def output = shOrClosure(script, command)
                logger.info("$output")
                return output
            }
        }
    }

    def tryExec(String action, def commands, def postCommands) {
        if (commands != null) {
            script.stage("${name}: ${action}") {
                try {
                    logger.debug("executing ", commands)
                    execute(commands)
                } catch (e) {
                    logger.error("Error when executing ${name} ${action}: ${commands}", e)
                    throw e
                } finally {
                    if (postCommands != null) {
                        try {
                            logger.debug("executing ", postCommands)
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
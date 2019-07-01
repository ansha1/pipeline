package com.nextiva.tools.build

import com.nextiva.tools.Tool
import com.nextiva.utils.Logger

import static com.nextiva.utils.Utils.shOrClosure

abstract class BuildTool implements Serializable, Tool {

    Script script
    String name
    String appName
    Logger log

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
        this.name = buildToolConfig.get("appName") ?: this.appName
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
        this.log = new Logger(this)

    }

    abstract String getVersion()

    abstract Boolean setVersion(String version)

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

    void publish() {
        tryExec("publishArtifact", publishCommands, null)
    }

    Boolean execute(def command) {
        script.dir(pathToSrc) {
            log.debug("executing command in container ${name}")
            script.container(name) {
                shOrClosure(script, command)
            }
        }
    }

    def tryExec(String action, def commands, def postCommands) {
        if (commands != null) {
            script.stage("${name}: ${action}") {
                try {
                    log.debug("executing ", commands)
                    execute(commands)
                } catch (e) {
                    log.error("Error when executing ${name} ${action}: ${commands}", e)
                    throw e
                } finally {
                    if (postCommands != null) {
                        try {
                            log.debug("executing ", postCommands)
                            execute(postCommands)
                        } catch (e) {
                            log.error("Error when executing ${name} ${action}: ${postCommands}", e)
                            throw e
                        }
                    }
                }
            }
        }
    }
}
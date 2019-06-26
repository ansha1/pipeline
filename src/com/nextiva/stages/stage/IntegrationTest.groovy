package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool


class IntegrationTest extends Stage {
    IntegrationTest(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        Map build = configuration.get("build")
        build.each {toolName, toolConfig ->
            withStage("${toolName} ${stageName}") {
                BuildTool tool = toolConfig.get("instance")
                try {
                    def integrationTestCommands = toolConfig.get("integrationTestCommands")
                    log.debug("executing ", integrationTestCommands)
                    tool.execute(integrationTestCommands)
                } catch (e) {
                    log.error("Error when executing ${toolName} ${stageName}:", e)
                    throw e
                } finally {
                    def postIntegrationTestCommands = toolConfig.get("postIntegrationTestCommands")
                    if (postIntegrationTestCommands != null) {
                        log.debug("executing ", postIntegrationTestCommands)
                        tool.execute(postIntegrationTestCommands)
                    }
                }
            }
        }
    }
}

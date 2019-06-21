package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool


class UnitTest extends Stage {
    UnitTest(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        Map build = configuration.get("build")
        build.each {toolName, toolConfig ->
            withStage("${toolName} ${stageName()}") {
                BuildTool tool = toolConfig.get("instance")
                try {
                    def unitTestCommands = toolConfig.get("unitTestCommands")
                    log.debug("executing ", unitTestCommands)
                    tool.execute(unitTestCommands)
                } catch (e) {
                    log.error("Error when executing ${toolName} ${stageName()}:", e)
                    throw e
                } finally {
                    def postUnitTestCommands = toolConfig.get("postUnitTestCommands")
                    if (postUnitTestCommands != null) {
                        log.debug("executing ", postUnitTestCommands)
                        tool.execute(postUnitTestCommands)
                    }
                }
            }
        }
    }
}
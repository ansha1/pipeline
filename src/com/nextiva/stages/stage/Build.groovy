package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool

class Build extends Stage {
    Build(Script script, Map configuration) {
        super(script, configuration)
    }

    def stageBody() {
        Map build = configuration.get("build")
        build.each {toolName, toolConfig ->
            withStage("${toolName} ${stageName}") {
                BuildTool tool = toolConfig.get("instance")
                try {
                    def buildCommands = toolConfig.get("buildCommands")
                    log.debug("executing ", buildCommands)
                    tool.execute(buildCommands)
                } catch (e) {
                    log.error("Error when executing ${toolName} ${stageName}:", e)
                    throw e
                } finally {
                    def postBuildCommands = toolConfig.get("postBuildCommands")
                    if (postBuildCommands != null) {
                        log.debug("executing ", postBuildCommands)
                        tool.execute(postBuildCommands)
                    }
                }
            }
        }
    }
}

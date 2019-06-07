package com.nextiva.stages.stage

import com.nextiva.build.tool.BuildTool

class Publish extends Stage {
    Publish(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        Map build = configuration.get("build")
        build.each { toolName, toolConfig ->
            if (toolConfig.get("publishArtifact")) {
                withStage("${toolName} ${stageName()}") {
                    BuildTool tool = toolConfig.get("tool")
                    try {
                        tool.publish()
                    } catch (e) {
                        log.error("Error when publishing with  ${toolName}:", e)
                        throw e
                    }
                }
            }
        }
    }
}
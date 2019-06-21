package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool


class SecurityScan extends Stage {
    SecurityScan(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        Map build = configuration.get("build")
        build.each { toolName, toolConfig ->
            withStage("${toolName} ${stageName()}") {
                BuildTool tool = toolConfig.get("instance")
                try {
                    tool.securityScan()
                } catch (e) {
                    log.error("Error when executing ${toolName} ${stageName()}:", e)
                    throw e
                }
            }
        }
    }
}

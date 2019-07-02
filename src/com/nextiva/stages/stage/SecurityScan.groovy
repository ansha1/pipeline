package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool


class SecurityScan extends Stage {
    SecurityScan(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        Map build = configuration.get("build")
        build.each { toolName, toolConfiguration ->
            BuildTool tool = toolConfiguration.get("instance")
            tool.securityScan()
        }
    }
}

package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool
import static com.nextiva.config.Config.instance as config

class SecurityScan extends Stage {
    SecurityScan() {
        super()
    }

    @Override
    def stageBody() {
        Map build = config.build
        build.each { toolName, toolConfiguration ->
            BuildTool tool = toolConfiguration.get("instance")
            tool.securityScan()
        }
    }
}

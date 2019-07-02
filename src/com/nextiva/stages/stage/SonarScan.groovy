package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool


class SonarScan extends Stage {
    SonarScan(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        Map build = configuration.get("build")
        build.each { toolName, toolConfiguration ->
            BuildTool tool = toolConfiguration.get("instance")
            tool.sonarScan()
        }
    }
}

package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool

class Build extends Stage {
    Build(Script script, Map configuration) {
        super(script, configuration)
    }

    def stageBody() {
        Map build = configuration.get("build")
        build.each { toolName, toolConfiguration ->
            BuildTool tool = toolConfiguration.get("instance")
            tool.build()
        }
    }
}

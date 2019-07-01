package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool

class Build extends Stage {
    Build(Script script, Map configuration) {
        super(script, configuration)
    }

    def stageBody() {
        Map build = configuration.get("build")
        build.each {
            BuildTool tool = it.get("instance")
            tool.build()
        }
    }
}

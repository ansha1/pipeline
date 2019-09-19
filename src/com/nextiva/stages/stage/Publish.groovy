package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool


class Publish extends Stage {
    Publish(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        Map build = configuration.get("build")
        for (entry in build) {
            Map toolConfiguration = entry.value
            BuildTool tool = toolConfiguration.get("instance")
            tool.publish()
        }
    }
}
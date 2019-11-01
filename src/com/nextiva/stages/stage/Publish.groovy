package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool
import static com.nextiva.config.Config.instance as config

class Publish extends Stage {
    Publish() {
        super()
    }

    @Override
    def stageBody() {
        Map build = config.build
        for (entry in build) {
            Map toolConfiguration = entry.value
            BuildTool tool = toolConfiguration.get("instance")
            tool.publish()
        }
    }
}
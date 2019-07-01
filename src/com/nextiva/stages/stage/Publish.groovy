package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool


class Publish extends Stage {
    Publish(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        Map build = configuration.get("build")
        build.each {
            BuildTool tool = it.get("instance")
            tool.publish()
        }
    }
}
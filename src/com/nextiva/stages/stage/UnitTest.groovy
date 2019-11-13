package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool
import static com.nextiva.config.Config.instance as config

class UnitTest extends Stage {
    UnitTest() {
        super()
    }

    @Override
    def stageBody() {
        List build = config.build
        build.each { toolConfiguration ->
            BuildTool tool = toolConfiguration.get("instance")
            tool.unitTest()
        }
    }
}
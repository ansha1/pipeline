package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool

class IntegrationTest extends Stage {
    IntegrationTest(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {

        //TODO: build docker based on current code. publish in nexus and run it

        Map build = configuration.get("build")
        build.each { toolName, toolConfiguration ->
            BuildTool tool = toolConfiguration.get("instance")
            tool.integrationTest()
        }
    }
}

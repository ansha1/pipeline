package com.nextiva.stages.stage

import com.nextiva.build.tool.BuildTool

class SonarScan extends Stage {
    SonarScan(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        Map build = configuration.get("build")
        build.each {
            BuildTool tool = it.get("tool")
            try {
                tool.sonarScan()
            } catch (e) {
                log.error("Error when executing ${name()}:", e)
                throw e
            }
        }
    }
}

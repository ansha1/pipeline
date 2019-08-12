package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool
import com.nextiva.tools.deploy.DeployTool

import static com.nextiva.config.Global.instance as global

class Deploy extends Stage {
    Deploy(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        for (environment in global.environmentsToDeploy) {
            doDeploy(global.deployTool, environment)
        }
        doPostDeploy()
    }

    private void doDeploy(DeployTool tool, environment) {
        withStage("$tool.name $stageName: Deploy to ${environment.name}") {
            try {
                tool.deploy(environment)
            } catch (e) {
                logger.error("Error when executing $tool.name $stageName:", e)
                throw e
            }
        }
    }

    private void doPostDeploy() {
        Map tools = configuration.get("build")
        tools.each { toolName, toolConfig ->
            BuildTool tool = toolConfig.get("instance")
            tool.postDeploy()
        }
    }
}

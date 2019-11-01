package com.nextiva.stages.stage

import com.nextiva.environment.Environment
import com.nextiva.tools.build.BuildTool
import com.nextiva.tools.deploy.DeployTool

import static com.nextiva.config.Config.instance as config

class Deploy extends Stage {
    Deploy() {
        super()
    }

    @Override
    def stageBody() {
        config.environmentsToDeploy.each {
            doDeploy(config.deployTool, it)
        }
        doPostDeploy()
    }

    private void doDeploy(DeployTool tool, Environment environment) {
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
        Map tools = config.build
        tools.each { toolName, toolConfig ->
            BuildTool tool = toolConfig.get("instance")
            tool.postDeploy()
        }
    }
}

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
        def m = [
                "NEW_RELIC_APP_ID"  : config.newRelicAppIdMap,
                "NEW_RELIC_APP_NAME": config.newRelicAppName,
                "BUILD_VERSION"     : config.version
        ]
        config.environmentsToDeploy.each {
            doDeploy(config.deployTool, it)
            config.script.newrelic.postDeployment(m, it.name)
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
        List tools = config.build
        tools.each { toolConfig ->
            BuildTool tool = toolConfig.get("instance")
            tool.postDeploy()
        }
    }
}

package com.nextiva.stages.stage

import com.nextiva.tools.deploy.DeployTool

import static com.nextiva.utils.Utils.shOrClosure

class Deploy extends Stage {
    Deploy(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        Map deploy = configuration.get("build")
        deploy.each { toolName, toolConfig ->
            withStage("${toolName} ${stageName}") {
                DeployTool tool = toolConfig.get("instance")
                try {
                    tool.deploy()
                    def postDeployCommands = toolConfig.get("postDeployCommands")
                    if (postDeployCommands != null) {
                        withStage("${toolName} ${stageName} postDeploy") {
                            def output = shOrClosure(script, postDeployCommands)
                            log.info("$output")
                        }
                    }
                } catch (e) {
                    log.error("Error when executing ${toolName} ${stageName}:", e)
                    throw e
                }
            }
        }
    }
}

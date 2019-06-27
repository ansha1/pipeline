package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool
import com.nextiva.utils.Logger


abstract class Stage implements Serializable {

    Script script
    String stageName
    Map configuration
    Logger log

    protected Stage(Script script, Map configuration) {
        this.script = script
        this.configuration = configuration
        this.stageName = this.getClass().getSimpleName()
        this.log = new Logger(this)
    }

    def execute() {
        log.trace("Executing stage stageName:${stageName} with configuration ${configuration}")
        withStage(stageName){
            stageBody()
        }
    }

    abstract def stageBody()


    def withStage(String stageName, def body){
        log.debug("Start executing $stageName stage")
        script.stage(stageName) {
            body()
        }
        log.debug("Execuiton $stageName stage complete")
    }

    def buildToolsCommandExecutor(Stage stage, Map toolMap, String commandsKey, String postCommandsKey){
        tooMap.each { toolName, toolConfig ->
            def commands = toolConfig.get(commandsKey)
            if (commands != null) {
                withStage("${stageName}: ${toolName}") {
                    BuildTool tool = toolConfig.get("instance")
                    try {
                        log.debug("executing ", commands)
                        tool.execute(commands)
                    } catch (e) {
                        log.error("Error when executing ${stageName} ${toolName} : ${commands}", e)
                        throw e
                    } finally {
                        def postCommands = toolConfig.get(postCommandsKey)
                        if (postCommands != null) {
                            try {
                                log.debug("executing ", postCommands)
                                tool.execute(postCommands)
                            } catch (e) {
                                log.error("Error when executing ${stageName} ${toolName} : ${postCommands}", e)
                                throw e
                            }
                        }
                    }
                }
            }
        }
    }
}
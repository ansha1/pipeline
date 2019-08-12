package com.nextiva.stages.stage

import com.nextiva.utils.Logger


abstract class Stage implements Serializable {

    Script script
    String stageName
    Map configuration
    Logger logger

    protected Stage(Script script, Map configuration) {
        this.script = script
        this.configuration = configuration
        this.stageName = this.getClass().getSimpleName()
        this.logger = new Logger(this)
    }

    def execute() {
        logger.trace("Executing stage stageName:${stageName} with configuration ${configuration}")
        withStage(stageName) {
            stageBody()
        }
    }

    abstract def stageBody()


    def withStage(String stageName, def body) {
        try {
            logger.debug("Start executing $stageName stage")
            script.stage(stageName) {
                body()
            }
            logger.debug("Execuiton $stageName stage complete")
        } catch (e) {
            logger.error("Error when executing ${stageName}:", e)
            throw e
        }
    }
}
package com.nextiva.stages.stage

import com.nextiva.utils.Logger
import static com.nextiva.config.Config.instance as config


abstract class Stage implements Serializable {

    String stageName
    Logger logger

    protected Stage() {
        this.stageName = this.getClass().getSimpleName()
        this.logger = new Logger(this)
    }

    def execute() {
        logger.info("Stage $stageName started.")
        logger.trace("Executing stage stageName: ${stageName} with config ${toString()}")
        withStage(stageName) {
            stageBody()
        }
        logger.info("Stage $stageName completed.")
    }

    abstract def stageBody()


    def withStage(String stageName, def body) {
        try {
            logger.debug("Start executing $stageName stage")
            config.script.stage(stageName) {
                body()
            }
            logger.debug("Execuiton $stageName stage complete")
        } catch (e) {
            logger.error("Error when executing ${stageName}:", e)
            throw e
        }
    }
}
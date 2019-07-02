package com.nextiva.stages.stage

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
        withStage(stageName) {
            stageBody()
        }
    }

    abstract def stageBody()


    def withStage(String stageName, def body) {
        try {
            log.debug("Start executing $stageName stage")
            script.stage(stageName) {
                body()
            }
            log.debug("Execuiton $stageName stage complete")
        } catch (e) {
            log.error("Error when executing ${stageName}:", e)
            throw e
        }
    }
}
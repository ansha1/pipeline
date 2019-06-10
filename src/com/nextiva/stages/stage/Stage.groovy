package com.nextiva.stages.stage

import com.nextiva.utils.Logger


abstract class Stage implements Serializable {

    Script script
    Map configuration
    Logger log

    protected Stage(Script script, Map configuration) {
        this.script = script
        this.configuration = configuration
        this.log = new Logger(this)
    }

    def execute() {
        withStage(stageName()){
            stageBody()
        }
    }

    abstract def stageBody()

    static String stageName() {
        return this.getClass().getSimpleName()
    }

    def withStage(String stageName, def body){
        log.debug("Start executing $stageName stage")
        script.stage(stageName) {
            body()
        }
        log.debug("Execuiton $stageName stage complete")
    }
}
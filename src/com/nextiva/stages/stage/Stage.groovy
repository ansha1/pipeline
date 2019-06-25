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
        withStage(){
            stageBody()
        }
    }

    abstract def stageBody()

    String stageName() {
        return stageName
    }

    def withStage(def body){
        log.debug("Start executing $stageName stage")
        script.stage(stageName()) {
            body()
        }
        log.debug("Execuiton $stageName stage complete")
    }
}
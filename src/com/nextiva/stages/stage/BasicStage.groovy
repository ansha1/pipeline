package com.nextiva.stages.stage


abstract class BasicStage implements Serializable {

    final protected script
    final protected Map configuration

    protected BasicStage(Script script, Map configuration) {
        this.script = script
        this.configuration = configuration
    }

    abstract execute()
}
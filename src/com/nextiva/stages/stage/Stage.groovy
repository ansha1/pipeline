package com.nextiva.stages.stage


abstract class Stage implements Serializable {

    final protected script

    protected Stage(Script script, Map configuration) {
        this.script = script
    }

    abstract execute()
}
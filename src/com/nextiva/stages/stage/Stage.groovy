package com.nextiva.stages.stage

import com.nextiva.utils.Logger


abstract class Stage implements Serializable {

    final protected script
    Map configuration
    Logger log = new Logger(this)

    protected Stage(Script script, Map configuration) {
        this.script = script
        this.configuration = configuration
    }

    abstract execute()

    @Override
    String toString(){
        return this.getClass().getSimpleName()
    }

}
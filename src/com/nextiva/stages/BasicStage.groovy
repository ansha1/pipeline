package com.nextiva.stages

abstract class BasicSage implements Serializable{
    final protected Map configuration
    final protected script

    protected BasicStage(script, configuration) {
        this.configuration = configuration
        this.script = script
    }

    abstract excute()


}

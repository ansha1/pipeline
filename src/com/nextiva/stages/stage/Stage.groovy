package com.nextiva.stages.stage


abstract class Stage implements Serializable {

    final protected script
    Map configuration

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
package com.nextiva.stages.stage

class DeployByAnsible extends Stage {
    DeployByAnsible(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def execute(){
        script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
    }
}
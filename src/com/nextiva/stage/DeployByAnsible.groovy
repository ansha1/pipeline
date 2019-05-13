package com.nextiva.stage

class DeployByAnsible extends BasicStage {
    protected DeployByAnsible(script, Configuration configuration) {
        super(script, configuration)
    }

    @Override
    def execute(){
        script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
    }
}
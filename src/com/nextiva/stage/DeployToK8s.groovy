package com.nextiva.stage

class DeployToK8s extends BasicStage {
    DeployToK8s(script, configuration) {
        super(script, configuration)
    }

    @Override
    def execute(){
        script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
    }
}
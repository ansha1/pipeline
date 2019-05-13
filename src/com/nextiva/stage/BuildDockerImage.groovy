package com.nextiva.stage

class BuildDockerImage extends BasicStage {
    BuildDockerImage(Script script, Map configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
        }
    }
}


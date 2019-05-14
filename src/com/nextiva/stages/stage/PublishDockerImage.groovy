package com.nextiva.stages.stage

class PublishDockerImage extends BasicStage {
    PublishDockerImage(Script script, Map configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
//           publishDockerImage
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
        }
    }
}

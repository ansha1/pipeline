package com.nextiva.stage

class PublishDockerImage extends BasicStage {
    PublishDockerImage(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
//           publishDockerImage
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
        }
    }
}

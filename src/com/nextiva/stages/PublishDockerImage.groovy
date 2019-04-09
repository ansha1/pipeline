package com.nextiva.stages

import com.nextiva.stages.BasicStage

class PublishDockerImage extends BasicStage {
    protected PublishDockerImage(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
//           publishDockerImage
        }
    }
}

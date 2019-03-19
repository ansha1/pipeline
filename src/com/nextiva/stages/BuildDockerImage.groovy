package com.nextiva.stages

import com.nextiva.stages.BasicStage

class BuildDockerImage extends BasicStage {
    protected BuildDockerImage(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
            utils.buildPublishDockerImage()
        }
    }
}


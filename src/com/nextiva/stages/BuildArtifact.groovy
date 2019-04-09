package com.nextiva.stages

import com.nextiva.stages.BasicStage

class BuildArtifact extends BasicStage {
    protected BuildArtifact(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
//buildArtifact
        }
    }
}

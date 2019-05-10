package com.nextiva.stage

class PublishArtifact extends BasicStage {
    protected PublishArtifact(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
//           publishArtifact
        }
    }
}

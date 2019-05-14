package com.nextiva.stages.stage

class PublishArtifact extends BasicStage {
    PublishArtifact(Script script, Map configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
//           publishArtifact
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
        }
    }
}

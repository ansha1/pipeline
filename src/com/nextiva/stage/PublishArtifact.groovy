package com.nextiva.stage

class PublishArtifact extends BasicStage {
    PublishArtifact(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
//           publishArtifact
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
        }
    }
}

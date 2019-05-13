package com.nextiva.stage

class BuildArtifact extends BasicStage {
    protected BuildArtifact(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
        }
    }
}

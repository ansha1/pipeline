package stages

import com.nextiva.stages.BasicStage

class PublishArtifact extends BasicStage {
    protected PublishArtifact(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
           publishArtifact
        }
    }
}

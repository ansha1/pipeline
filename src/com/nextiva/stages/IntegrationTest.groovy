package stages

import com.nextiva.stages.BasicStage

class IntegrationTest extends BasicStage {
    protected IntegrationTest(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
           //integration test stage
        }
    }
}

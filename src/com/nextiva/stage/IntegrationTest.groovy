package com.nextiva.stage

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

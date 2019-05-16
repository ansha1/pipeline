package com.nextiva.stages.stage

class IntegrationTest extends Stage {
    IntegrationTest(Script script, Map configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
           //integration test stage
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
        }
    }
}

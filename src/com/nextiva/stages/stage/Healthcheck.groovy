package com.nextiva.stages.stage

class Healthcheck extends BasicStage {
    Healthcheck(Script script, Map configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
            //blackbox healthcheck ensure than service is upp and running also validate that version is correct
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
        }
    }
}

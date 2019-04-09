package com.nextiva.stages

import com.nextiva.stages.BasicStage

class Healthcheck extends BasicStage {
    protected Healthcheck(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
            //blackbox healthcheck ensure than service is upp and running also validate that version is correct
        }
    }
}

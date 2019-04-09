package com.nextiva.stages

import com.nextiva.stages.BasicStage

class Deploy extends BasicStage {
    protected Deploy(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
            //deploy on kubernetes environment
        }
    }
}

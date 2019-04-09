package com.nextiva.stages

import com.nextiva.stages.BasicStage

class QACoreTeamTest extends BasicStage {
    protected QACoreTeamTest(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
           // execute qa core team tests build
        }
    }
}

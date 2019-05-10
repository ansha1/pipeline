package com.nextiva.stage

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

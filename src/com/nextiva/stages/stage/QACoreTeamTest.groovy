package com.nextiva.stages.stage

class QACoreTeamTest extends BasicStage {
    QACoreTeamTest(Script script, Map configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
           // execute qa core team tests build
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
        }
    }
}

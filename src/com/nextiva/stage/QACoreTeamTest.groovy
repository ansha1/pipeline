package com.nextiva.stage

class QACoreTeamTest extends BasicStage {
    QACoreTeamTest(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
           // execute qa core team tests build
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
        }
    }
}

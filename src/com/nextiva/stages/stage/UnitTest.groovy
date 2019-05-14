package com.nextiva.stages.stage

import com.nextiva.stages.stage.BasicStage

class UnitTest extends BasicStage {
    UnitTest(Script script, Map configuration) {
        super(script, configuration)
    }

    def execute() {
        script.stage(this.getClass().getSimpleName()) {
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
//        script.stage(this.getClass().getSimpleName()) {
//           utils.build()
//        }
        }
    }
}

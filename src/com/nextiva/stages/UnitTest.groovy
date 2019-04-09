package com.nextiva.stages

import com.nextiva.stages.BasicStage

class UnitTest extends BasicStage {
    protected UnitTest(script, configuration) {
        super(script, configuration)
    }

    def execute() {
        script.stage(this.getClass().getSimpleName()) {
//        script.stage(this.getClass().getSimpleName()) {
//           utils.build()
//        }
        }
    }
}

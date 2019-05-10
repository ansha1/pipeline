package com.nextiva.stage

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

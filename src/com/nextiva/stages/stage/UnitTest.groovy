package com.nextiva.stages.stage

class UnitTest extends Stage {
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

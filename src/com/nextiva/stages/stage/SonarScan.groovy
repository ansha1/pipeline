package com.nextiva.stages.stage

class SonarScan extends Stage {
    SonarScan(Script script, Map configuration) {
        super(script, configuration)
    }

    def execute() {
        script.stage(this.getClass().getSimpleName()) {
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
        }
    }
}

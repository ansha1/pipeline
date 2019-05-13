package com.nextiva.stage

class SonarScan extends BasicStage {
    SonarScan(Script script, Map configuration) {
        super(script, configuration)
    }

    def execute() {
        script.stage(this.getClass().getSimpleName()) {
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
        }
    }
}

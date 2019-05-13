package com.nextiva.stage

class SonarScan extends BasicStage {
    SonarScan(script, configuration) {
        super(script, configuration)
    }

    def execute() {
        script.stage(this.getClass().getSimpleName()) {
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
            //sonar
//            if (jobConfig.DEPLOY_ONLY ==~ false && env.BRANCH_NAME ==~ /^(develop|dev)$/) {
//                utils.runSonarScanner(jobConfig.BUILD_VERSION)
//            }
        }
    }
}

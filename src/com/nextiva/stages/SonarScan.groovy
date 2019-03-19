package stages

import com.nextiva.stages.BasicStage

class SonarScan extends BasicStage {
    protected SonarScan(script, configuration) {
        super(script, configuration)
    }

    def execute() {
        script.stage(this.getClass().getSimpleName()) {
            if (jobConfig.DEPLOY_ONLY ==~ false && env.BRANCH_NAME ==~ /^(develop|dev)$/) {
                utils.runSonarScanner(jobConfig.BUILD_VERSION)
            }
        }
    }
}

sss

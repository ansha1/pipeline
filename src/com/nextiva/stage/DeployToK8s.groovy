package com.nextiva.stage

class DeployToK8s extends BasicStage {
    protected DeployToK8s(script, configuration) {
        super(script, configuration)
    }

    @Override
    def execute(){
        script.sh(
                script: "deploy to k8s",
                returnStdout: true
        ).trim()
    }
}
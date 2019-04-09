package com.nextiva.stages

import com.nextiva.stages.BasicStage


class DeployByAnsible extends BasicStage {
    protected DeployByAnsible(script, configuration) {
        super(script, configuration)
    }

    @Override
    def execute(){
        script.sh(
                script: "deploy by Ansible",
                returnStdout: true
        ).trim()
    }
}
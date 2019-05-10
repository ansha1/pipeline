package com.nextiva.stage

class DeployByAnsible extends BasicStage {
    protected DeployByAnsible(script, Configuration configuration) {
        super(script, configuration)
    }

    @Override
    def execute(){
        configuration.ansible
        script.sh(
                script: "deploy by Ansible",
                returnStdout: true
        ).trim()
    }
}
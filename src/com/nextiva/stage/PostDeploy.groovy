package com.nextiva.stage

class PostDeploy extends BasicStage {
    protected PostDeploy(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
            //execute post deploy step might be some e2e tests or additional service validation
        }
    }
}

package com.nextiva.stages.stage

class SendNotifications extends BasicStage {
    SendNotifications(Script script, Map configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
//sendNotifications
        }
    }
}

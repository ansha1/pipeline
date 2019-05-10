package com.nextiva.stage

class SendNotifications extends BasicStage {
    protected SendNotifications(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
//sendNotifications
        }
    }
}

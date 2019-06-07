package com.nextiva.stages.stage

class QACoreTeamTest extends Stage {
    QACoreTeamTest(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        withStage("QA Core Team Tests") {
            //after successfully deploy on environment start QA CORE TEAM Integration and smoke tests with this application
            List environmentsToDeploy = configuration.get("environmentsToDeploy")
            environmentsToDeploy.each {
                script.build job: 'test-runner-on-deploy/develop', parameters: [script.string(name: 'Service', value: configuration.get("appName")),
                                                                                script.string(name: 'env', value: it.getName())], wait: false
            }
        }
    }
}

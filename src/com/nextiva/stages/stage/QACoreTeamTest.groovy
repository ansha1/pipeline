package com.nextiva.stages.stage

import static com.nextiva.config.Config.instance as config

class QACoreTeamTest extends Stage {
    QACoreTeamTest() {
        super()
    }

    @Override
    def stageBody() {
        withStage("QA Core Team Tests") {
            //after successfully deploy on environment start QA CORE TEAM Integration and smoke tests with this application
            config.environmentsToDeploy.each {
                config.script.build(
                        job: 'test-runner-on-deploy/develop',
                        parameters: [config.script.string(name: 'Service', value: config.appName),
                                     config.script.string(name: 'env', value: it.getName())],
                        wait: false
                )
            }
        }
    }
}

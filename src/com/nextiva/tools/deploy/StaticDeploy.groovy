package com.nextiva.tools.deploy

import com.nextiva.environment.Environment
import static com.nextiva.config.Global.instance as global

class StaticDeploy extends DeployTool {

    StaticDeploy(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    void deploy(Environment environment) {
        logger.info('Deploying using Ansible Static Deploy playbook')
        script.staticDeploy(global.appName, environment.name, global.globalVersion)
    }
}

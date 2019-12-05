package com.nextiva.tools.deploy

import com.nextiva.environment.Environment
import static com.nextiva.config.Config.instance as config

class StaticDeploy extends DeployTool {

    StaticDeploy(Map configuration) {
        super(configuration)
    }

    @Override
    void deploy(Environment environment) {
        logger.info('Deploying using Ansible Static Deploy playbook')
        config.script.staticDeploy(config.appName, environment.name, config.version)
    }
}

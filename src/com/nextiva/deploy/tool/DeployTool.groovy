package com.nextiva.deploy.tool

import com.nextiva.environment.Environment

abstract class DeployTool implements Serializable {

    Script script
    Map configuration

    String name
    String image
    String repository
    String branch
    List<Environment> environments

    DeployTool(Script script, List<Environment> environments, Map configuration) {
        this.script = script
        this.configuration = configuration
        this.environments = environments
        this.name = configuration.get("deploy")
    }

    Boolean health() {
        script.healthCheck.list(healthCheck)
        return true
    }

    abstract Boolean deploy()
}
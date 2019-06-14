package com.nextiva.deploy.tool

import com.nextiva.environment.Environment
import com.nextiva.utils.Logger

abstract class DeployTool implements Serializable {

    Script script
    Map configuration
    Logger log

    String name
    String repository
    String branch
    List<Environment> environments

    DeployTool(Script script, List<Environment> environments, Map configuration) {
        this.script = script
        this.configuration = configuration
        this.environments = environments
        this.name = configuration.get("deploy")
        this.log = new Logger(this)
    }

    Boolean health() {
        script.healthCheck.list(healthCheck)
        return true
    }

    String getName() {
        return this.getClass().getSimpleName().toLowerCase()
    }

    String getToolHome(){
        return "deploy/${getName()}"
    }

    abstract Boolean deploy()

    abstract void init()
}
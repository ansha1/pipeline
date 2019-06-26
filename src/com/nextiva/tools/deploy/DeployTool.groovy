package com.nextiva.tools.deploy

import com.nextiva.tools.Tool
import com.nextiva.utils.Logger

abstract class DeployTool implements Serializable, Tool {

    Script script
    String name
    Logger log
    String toolHome
    Boolean initialized = false

    String repository
    String branch

    DeployTool(Script script, Map deployToolConfig) {
        this.script = script
        this.name = deployToolConfig.get("name")
        this.toolHome = "${script.env.WORKSPACE}/deploy/${name}"
        this.repository = deployToolConfig.get("repository")
        this.branch = deployToolConfig.get("branch")
        this.log = new Logger(this)
        log.debug("created tool - name: ${this.name}, toolHome:${this.toolHome}, repository: ${this.repository}, branch: ${this.branch}")
    }

    Boolean isInitialized(){
        return initialized
    }

    Boolean health() {
        script.healthCheck.list(healthCheck)
        return true
    }
}
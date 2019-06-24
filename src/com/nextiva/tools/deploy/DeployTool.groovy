package com.nextiva.tools.deploy

import com.nextiva.tools.Tool
import com.nextiva.utils.Logger

abstract class DeployTool implements Serializable, Tool {

    Script script
    String name
    String toolHome
    Logger log

    String repository
    String branch
    Boolean initialized = false

    DeployTool(Script script, Map deployToolConfig) {
        this.script = script
        this.name = deployToolConfig.get("name")
        this.toolHome = "${script.env.WORKSPACE}/deploy/${name}"
        this.repository = deployToolConfig.get("repository")
        this.branch = deployToolConfig.get("branch")
        this.log = new Logger(this)
    }

    Boolean health() {
        script.healthCheck.list(healthCheck)
        return true
    }
}
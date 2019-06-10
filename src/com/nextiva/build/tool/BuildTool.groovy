package com.nextiva.build.tool

import com.nextiva.utils.Logger
import static com.nextiva.utils.Utils.shOrClosure

abstract class BuildTool {

    Script script
    Map toolConfiguration

    String pathToSrc = "."
    String version
    Boolean publishArtifact

    BuildTool(Script script, Map toolConfiguration) {
        this.script = script
        this.toolConfiguration = toolConfiguration
    }

    abstract void setVersion(String version)

    abstract String getVersion()

    Boolean execute(def command) {
        script.dir(pathToSrc) {
            log.debug("executing tool command in container ${this.getClass().getSimpleName().toLowerCase()}")
            script.container(this.getClass().getSimpleName().toLowerCase()) {
                shOrClosure(script, command)
            }
        }
    }

    abstract void sonarScan()
    abstract void securityScan()
    abstract void publish()

    abstract Boolean isArtifactAvailableInRepo()

}
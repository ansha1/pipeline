package com.nextiva.build.tool

import com.nextiva.Version
import com.nextiva.utils.Logger
import static com.nextiva.utils.Utils.shOrClosure

abstract class BuildTool {
    Logger log = new Logger(this)
    Script script
    Map toolConfiguration

    String pathToSrc = "."
    Version version
    Boolean publishArtifact

    BuildTool(Script script, Map toolConfiguration) {
        this.script = script
        this.toolConfiguration = toolConfiguration
    }

    abstract void setVersion(String version)

    abstract String getVersion()

    Boolean execute(def command) {
        script.dir(pathToSrc) {
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
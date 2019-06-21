package com.nextiva.tools.build

import com.nextiva.tools.Tool

import static com.nextiva.utils.Utils.shOrClosure

abstract class BuildTool implements Serializable, Tool {

    Script script
    String name

    Map toolConfiguration

    String pathToSrc = "."
    String version
    Boolean publishArtifact

    BuildTool(Script script, Map toolConfiguration) {
        this.script = script
        this.name = toolConfiguration.get("name")
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

    String getName(){
        return name
    }
    abstract void sonarScan()
    abstract void securityScan()
    abstract void publish()

    abstract Boolean isArtifactAvailableInRepo()

}
package com.nextiva.tools.build

import com.nextiva.tools.Tool
import com.nextiva.utils.Logger

import static com.nextiva.utils.Utils.shOrClosure

abstract class BuildTool implements Serializable, Tool {

    Script script
    String name
    Logger log

    String pathToSrc

    BuildTool(Script script, Map buildToolConfig) {
        this.script = script
        this.name = buildToolConfig.get("name")
        this.pathToSrc = buildToolConfig.get("pathToSrc", ".")
        this.log = new Logger(this)
    }

    Boolean execute(def command) {
        script.dir(pathToSrc) {
            log.debug("executing command in container ${name}")
            script.container(getName()) {
                shOrClosure(script, command)
            }
        }
    }


    abstract String getVersion()

    abstract Boolean setVersion(String version)

    abstract void sonarScan()

    abstract void securityScan()

    abstract void publish()

    abstract Boolean isArtifactAvailableInRepo()

}
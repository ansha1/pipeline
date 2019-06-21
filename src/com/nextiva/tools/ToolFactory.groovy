package com.nextiva.tools

import com.nextiva.tools.build.Docker
import com.nextiva.tools.build.Maven
import com.nextiva.tools.build.Npm
import com.nextiva.tools.build.Pip
import com.nextiva.tools.deploy.Ansible
import com.nextiva.tools.deploy.Kubeup
import com.nextiva.utils.Logger
import hudson.AbortException

import static com.nextiva.SharedJobsStaticVars.DEFAULT_TOOL_CONFIGURATION

class ToolFactory {
    Logger log = new Logger(this)


    Tool build(Script script, Map toolConfig) {
        switch (toolConfig.get("name")) {
            case "docker":
                return new Docker(script, toolConfig)
                break
            case "maven":
                return new Maven(script, toolConfig)
                break
            case "npm":
                return new Npm(script, toolConfig)
                break
            case "pip":
                return new Pip(script, toolConfig)
                break
            case "ansible":
                return new Ansible(script, toolConfig)
                break
            case "kubeup":
                return new Kubeup(script, toolConfig)
                break
            default:
                log.error("Can't create tool from", toolConfig)
                throw new AbortException("Can't create deployment class from $toolConfig")
        }
    }

    Map buildAndPutInMap(Script script, Map toolConfig) {
        toolConfig = mergeWithDefaults(toolConfig)
        Tool instance = build(script, toolConfig)
        toolConfig.put("instance", instance)
        return toolConfig
    }

    Map mergeWithDefaults(Map toolConfig) {
        String tool = toolConfig.get("name")
        log.debug("got tool $tool")
        Map defaultConfig = DEFAULT_TOOL_CONFIGURATION.get(tool)
        log.debug("got default tool config", defaultConfig)
        Map result = defaultConfig << toolConfig
        log.debug("toolСonfig after merge", toolConfig)
        return result
    }
}

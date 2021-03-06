package com.nextiva.tools

import com.nextiva.tools.build.Docker
import com.nextiva.tools.build.Make
import com.nextiva.tools.build.Maven
import com.nextiva.tools.build.Npm
import com.nextiva.tools.build.Python
import com.nextiva.tools.deploy.Ansible
import com.nextiva.tools.deploy.Kubeup
import com.nextiva.tools.deploy.StaticDeploy
import com.nextiva.utils.Logger
import hudson.AbortException

import static com.nextiva.SharedJobsStaticVars.DEFAULT_TOOL_CONFIGURATION

class ToolFactory {
    Logger logger = new Logger(this)

    Tool build(Map toolConfig) {
        switch (toolConfig.get("name")) {
            case "docker":
                return new Docker(toolConfig)
                break
            case "maven":
                return new Maven(toolConfig)
                break
            case "npm":
                return new Npm(toolConfig)
                break
            case "python":
                return new Python(toolConfig)
                break
            case "ansible":
                Ansible ansible = new Ansible(toolConfig)
                logger.trace("Created tool Ansible", ansible)
                return ansible
                break
            case "kubeup":
                Kubeup kubeup = new Kubeup(toolConfig)
                logger.trace("Created tool Kubeup", kubeup)
                return kubeup
                break
            case "static":
                StaticDeploy staticDeploy = new StaticDeploy(toolConfig)
                logger.trace("Created tool StaticDeploy", staticDeploy)
                return staticDeploy
                break
            case "make":
                return new Make(toolConfig)
                break
            default:
                logger.error("Can't create tool from", toolConfig)
                throw new AbortException("Can't create tool from $toolConfig")
        }
    }

    void mergeWithDefaults(Map toolConfig) {
        String tool = toolConfig.get("name")
        logger.debug("got tool $tool")
        Map defaultConfig = DEFAULT_TOOL_CONFIGURATION.get(tool)
        if (defaultConfig == null) {
            throw new AbortException("There is no configuration for this tool $tool, aborting...")
        }
        logger.debug("got default tool config", defaultConfig)
        toolConfig << (defaultConfig + toolConfig)
        logger.debug("toolСonfig after merge", toolConfig)
    }
}

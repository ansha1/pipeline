package com.nextiva.config

import com.cloudbees.groovy.cps.NonCPS

import static com.nextiva.SharedJobsStaticVars.DEFAULT_TOOL_CONFIGURATION

class SlaveFactory {
    protected Script script
    private Map slaveConfig = [:]

    SlaveFactory(Script script, Map configuration) {
        this.script = script
        Map containerResources = [:]
        containerResources.put("jnlp", configuration.get("jenkinsContainer", DEFAULT_CONTAINERS.get("jnlp")))
        Map dependencies = configuration.get("dependencies")
        if (dependencies) {
            containerResources.put("kubeup", DEFAULT_CONTAINERS.get("kubeup"))
        }
        Map buildTools = configuration.get("build")
        if (buildTools) {
            containerResources << configureToolContainers(buildTools)
        }

        Map deployTools = configuration.get("deploy")
        if (deployTools) {
            containerResources << configureToolContainers(deployTools)
        }
        slaveConfig.put("containerResources", containerResources)
    }

    @NonCPS
    Map configureToolContainers(Map<String, Map> tools) {
        Map toolContainers = [:]
        tools.each { tool, toolConfiguration ->
            Map toolContainerConfiguration = DEFAULT_CONTAINERS.get(tool)
            toolContainerConfiguration << toolConfiguration.subMap(["image",
                                                                    "resourceRequestCpu",
                                                                    "resourceLimitCpu",
                                                                    "resourceRequestMemory",
                                                                    "resourceLimitMemory",
            ]).findAll {
                it.value
            }
            toolContainers.put(tool, toolContainerConfiguration)
        }
        return toolContainers
    }

    @NonCPS
    Map getSlaveConfiguration() {
        return slaveConfig
    }

}

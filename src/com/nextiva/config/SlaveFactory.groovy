package com.nextiva.config

import com.cloudbees.groovy.cps.NonCPS

import static com.nextiva.SharedJobsStaticVars.DEFAULT_CONTAINERS

class SlaveFactory {
    protected Script script
    private Map slaveConfig = [:]

    SlaveFactory(Script script, Map configuration) {
        this.script = script
        Map containerResources = [:]
        containerResources.put("jnlp", configuration.get("jenkinsContainer", DEFAULT_CONTAINERS.get("jnlp")))
        echo("1containerResources $containerResources")
        Map dependencies = configuration.get("dependencies")
        if (dependencies) {
            containerResources.put("kubeup", DEFAULT_CONTAINERS.get("kubeup"))
        }
        echo("2containerResources $containerResources")

        Map buildTools = configuration.get("build")
        echo("buildTools $buildTools")
        if (buildTools) {
            containerResources << configureToolContainers(buildTools)
        }
        echo("3containerResources $containerResources")

        Map deployTools = configuration.get("deploy")
        if (deployTools) {
            containerResources << configureToolContainers(deployTools)
        }
        echo("4containerResources $containerResources")
        slaveConfig.put("containerResources", containerResources)
        echo("slaveConfig $slaveConfig")
    }

    Map configureToolContainers(Map<String, Map> tools) {
        Map toolContainers = [:]
        tools.each { tool, toolConfiguration ->
            echo("tool $tool toolConfiguration $toolConfiguration")
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

    Map getSlaveConfiguration() {
        script.echo("slaveconfig is $slaveConfig<<")
        return slaveConfig
    }

    @NonCPS
    protected echo(msg) {
        script.echo("[${this.getClass().getSimpleName()}] ${msg}")
    }
}

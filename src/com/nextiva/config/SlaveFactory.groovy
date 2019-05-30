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
echo("1")
        Map dependencies = configuration.get("dependencies")
        if (dependencies) {
            echo("deps $dependencies")
            containerResources.put("kubeup", DEFAULT_CONTAINERS.get("kubeup"))
        }
        echo("2")

        Map buildTools = configuration.get("build")
        if (buildTools) {
            containerResources << configureToolContainers(buildTools)
        }

        echo("3")
        echo("3")
        Map deployTools = configuration.get("deploy")
        if (deployTools) {
            containerResources << configureToolContainers(deployTools)
        }
        slaveConfig.put("containerResources", containerResources)
    }

    private static Map configureToolContainers(Map<String, Map> tools) {
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

    Map getSlaveConfiguration() {
        script.echo("slaveconfig is $slaveConfig<<")
        return slaveConfig
    }

    @NonCPS
    protected echo(msg) {
        script.echo("[${this.getClass().getSimpleName()}] ${msg}")
    }
}

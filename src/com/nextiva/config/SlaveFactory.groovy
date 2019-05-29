package com.nextiva.config

import static com.nextiva.SharedJobsStaticVars.getDEFAULT_CONTAINERS

class SlaveFactory {
    private Map slaveConfig = [:]

    SlaveFactory(Config config) {
        Map containerResources = [:]
        containerResources.put("jnlp", config.getJenkinsContainer())

        Map dependencies = config.getBuildDependencies()
        if (dependencies) {
            containerResources.put("kubeup", getDEFAULT_CONTAINERS().get("kubeup"))
        }

        Map buildTools = config.getBuildConfiguration()
        if (buildTools) {
            containerResources << configureToolContainers(buildTools)
        }

        Map deployTools = config.getDeployConfiguration()
        if (deployTools) {
            containerResources << configureToolContainers(deployTools)
        }

        slaveConfig.put("containerResources", containerResources)
    }

    private static Map configureToolContainers(Map<String, Map> tools) {
        Map toolContainers = [:]
        tools.each { tool, toolConfiguration ->
            Map toolContainerConfiguration = getDEFAULT_CONTAINERS().get(tool)
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
        return slaveConfig
    }
}

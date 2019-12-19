package com.nextiva.stages.stage

import com.nextiva.tools.ToolFactory
import com.nextiva.tools.build.BuildTool
import com.nextiva.tools.build.Docker
import com.nextiva.tools.deploy.Kubeup
import hudson.AbortException

import static com.nextiva.SharedJobsStaticVars.NEXTIVA_DOCKER_TEST_REGISTRY_URL
import static com.nextiva.SharedJobsStaticVars.NEXTIVA_DOCKER_REGISTRY_CREDENTIALS_ID
import static com.nextiva.SharedJobsStaticVars.JENKINS_KUBERNETES_CLUSTER_DOMAIN
import static com.nextiva.config.Config.instance as config

class IntegrationTest extends Stage {
    IntegrationTest() {
        super()
    }

    @Override
    def stageBody() {
        //TODO: build docker based on current code. publish in nexus and run it
        logger.debug("Building docker test image ")
        List build = config.build
        Map dockerToolConfig = build.find { it.name == "docker" }
        if (dockerToolConfig == null) {
            logger.error("docker build tool is undefined, can't build test container image, aborting...")
            throw new AbortException("docker build tool is undefined, can't build test container image, aborting...")
        }
        Docker docker = dockerToolConfig.get("instance")
        // TODO add multistage builds support
        docker.execute {
            docker.buildPublish(NEXTIVA_DOCKER_TEST_REGISTRY_URL,
                    NEXTIVA_DOCKER_REGISTRY_CREDENTIALS_ID, config.appName, config.version)
        }
        logger.debug("Build and publish success")

        ToolFactory toolFactory = new ToolFactory()
        Map toolMap = ["name": "kubeup"]
        toolFactory.mergeWithDefaults(toolMap)
        Kubeup kubeup = toolFactory.build(toolMap)
        kubeup.init(JENKINS_KUBERNETES_CLUSTER_DOMAIN)
        String configset = "test"
        config.script.withEnv(["CLUSTER_DOMAIN=${config.ciClusterDomain}"]) {
            kubeup.install(config.appName, config.version, config.namespace, configset, false)
        }

        build.each { toolConfiguration ->
            BuildTool tool = toolConfiguration.get("instance")
            tool.integrationTest()
        }
    }
}

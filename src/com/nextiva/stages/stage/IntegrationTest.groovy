package com.nextiva.stages.stage

import com.nextiva.tools.ToolFactory
import com.nextiva.tools.build.BuildTool
import com.nextiva.tools.build.Docker
import com.nextiva.tools.deploy.Kubeup
import hudson.AbortException

import static com.nextiva.SharedJobsStaticVars.NEXTIVA_DOCKER_TEST_REGISTRY_URL
import static com.nextiva.SharedJobsStaticVars.NEXTIVA_DOCKER_REGISTRY_CREDENTIALS_ID
import static com.nextiva.SharedJobsStaticVars.JENKINS_KUBERNETES_CLUSTER_DOMAIN
import static com.nextiva.utils.Utils.buildID
import static com.nextiva.utils.Utils.getGlobal
import static com.nextiva.utils.Utils.getGlobalVersion

class IntegrationTest extends Stage {
    IntegrationTest(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        //TODO: build docker based on current code. publish in nexus and run it
        logger.debug("Building docker test image ")
        Map build = configuration.get("build")
        String appName = getGlobal().appName
        String version = getGlobalVersion()
        Map dockerToolConfig = build.get("docker")
        if (dockerToolConfig == null) {
            logger.error("docker build tool is undefined, can't build test container image, aborting...")
            throw new AbortException("docker build tool is undefined, can't build test container image, aborting...")
        }
        Docker docker = dockerToolConfig.get("instance")
        // TODO add multistage builds support
        docker.execute {
            docker.buildPublish(script, NEXTIVA_DOCKER_TEST_REGISTRY_URL, NEXTIVA_DOCKER_REGISTRY_CREDENTIALS_ID, appName, version)
        }
        logger.debug("Build and publish success")

        ToolFactory toolFactory = new ToolFactory()
        Map toolMap = ["name": "kubeup"]
        toolFactory.mergeWithDefaults(toolMap)
        Kubeup kubeup = toolFactory.build(script, toolMap)
        String clusterDomain = JENKINS_KUBERNETES_CLUSTER_DOMAIN
        kubeup.init(clusterDomain)
        String namespace = buildID(script.env.JOB_NAME, script.env.BUILD_ID)
        String configset = "test"
        String ciClusterDomain = "$namespace-$JENKINS_KUBERNETES_CLUSTER_DOMAIN"
        script.withEnv(["CLUSTER_DOMAIN=$ciClusterDomain"]) {
            kubeup.install(appName, version, namespace, configset, false)
        }

        build.each { toolName, toolConfiguration ->
            BuildTool tool = toolConfiguration.get("instance")
            tool.integrationTest()
        }
    }
}

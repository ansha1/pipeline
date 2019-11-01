package com.nextiva.stages.stage

import com.nextiva.tools.ToolFactory
import com.nextiva.tools.deploy.Kubeup

import static com.nextiva.utils.Utils.buildID
import static com.nextiva.SharedJobsStaticVars.JENKINS_KUBERNETES_CLUSTER_DOMAIN
import static com.nextiva.config.Config.instance as config


class StartBuildDependencies extends Stage {
    StartBuildDependencies() {
        super()
    }

    @Override
    def stageBody() {
        ToolFactory toolFactory = new ToolFactory()
        Map toolMap = ["name": "kubeup"] + config.kubeupConfig
        toolFactory.mergeWithDefaults(toolMap)
        Kubeup kubeup = toolFactory.build(toolMap)
        String clusterDomain = JENKINS_KUBERNETES_CLUSTER_DOMAIN
        kubeup.init(clusterDomain)
        config.dependencies.each { cloudApp, version ->
            String namespace = buildID(config.script.env.JOB_NAME, config.script.env.BUILD_ID)
            String configset = "test"
            String ciClusterDomain = "$namespace-$JENKINS_KUBERNETES_CLUSTER_DOMAIN"
            config.script.withEnv(["CLUSTER_DOMAIN=$ciClusterDomain"]) {
                kubeup.install(cloudApp, version, namespace, configset, false)
            }
        }
    }
}

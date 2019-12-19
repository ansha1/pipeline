package com.nextiva.stages.stage

import com.nextiva.tools.ToolFactory
import com.nextiva.tools.deploy.Kubeup

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
        kubeup.init(JENKINS_KUBERNETES_CLUSTER_DOMAIN)
        config.dependencies.each { cloudApp, version ->
            String configset = "test"
            config.script.withEnv(["CLUSTER_DOMAIN=${config.ciClusterDomain}"]) {
                kubeup.install(cloudApp, version, config.namespace, configset, false)
            }
        }
    }
}

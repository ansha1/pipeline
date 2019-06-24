package com.nextiva.stages.stage

import com.nextiva.tools.ToolFactory
import com.nextiva.tools.deploy.Kubeup

import static com.nextiva.utils.Utils.buildID
import static com.nextiva.SharedJobsStaticVars.JENKINS_KUBERNETES_CLUSTER_DOMAIN

class StartBuildDependencies extends Stage {
    StartBuildDependencies(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        try {
            Map<String, String> dependencies = configuration.get("dependencies")
            ToolFactory toolFactory = new ToolFactory()
            Map toolMap = toolFactory.mergeWithDefaults(["name": "kubeup"])
            Kubeup kubeup = toolFactory.build(script, toolMap)
            String clusterDomain = JENKINS_KUBERNETES_CLUSTER_DOMAIN
            kubeup.init(clusterDomain)
            dependencies.each { cloudApp, version ->
                String namespace = buildID(script.env.JOB_NAME, script.env.BUILD_ID)
                //TODO: change configset
                String configset = "aws-sandbox"
                String ciClusterDomain = "$namespace-$JENKINS_KUBERNETES_CLUSTER_DOMAIN"
                script.withEnv("CLUSTER_DOMAIN=$ciClusterDomain") {
                    kubeup.deploy(cloudApp, version, namespace, configset)
                }
            }
        } catch (e) {
            log.error("Error when executing ${stageName()}:", e)
            throw e
        }
    }
}

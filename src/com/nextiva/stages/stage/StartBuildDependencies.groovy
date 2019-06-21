package com.nextiva.stages.stage

import static com.nextiva.utils.Utils.buildID
import static com.nextiva.SharedJobsStaticVars.JENKINS_KUBERNETES_CLUSTER_DOMAIN

class StartBuildDependencies extends Stage {
    StartBuildDependencies(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        try {
            Map dependencies = configuration.get("dependencies")
            dependencies.each { cloudApp, version ->
                String clusterDomain = JENKINS_KUBERNETES_CLUSTER_DOMAIN
                String namespace = buildID(script.env.JOB_NAME, script.env.BUILD_ID)
                log.info("Starting build dependency $cloudApp")


                script.container("kubeup") {
                    log.debug("start kubernetes deploy with appname=$appName, version=$version, clusterDomain=$clusterDomain, deploymentList =$deploymentsList")
                    script.kubernetes.deploy(appName, version, clusterDomain, deploymentsList, namespace)
                }
            }
        } catch (e) {
            log.error("Error when executing ${stageName()}:", e)
            throw e
        }
    }
}

package com.nextiva.config

import static com.nextiva.SharedJobsStaticVars.*

class Config implements Serializable {
    private Map pipelineParams = new HashMap()
    private Script script


    protected Config(Script script, Map pipelineParams) {
        this.script = script
        this.pipelineParams << pipelineParams
        validate()
        setDefaults()
        collectJobParameters()
        configureSlave()
    }

    void validate() {
        // Checking mandatory variables
        List<String> configurationErrors = []

        if (!pipelineParams.containsKey("appName")) {
            configurationErrors.add("Application Name is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }
        if (!pipelineParams.containsKey("channelToNotify")) {
            configurationErrors.add("Slack notification channel is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }
        if (!pipelineParams.containsKey("buildTool")) {
            configurationErrors.add("BuildTool is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }
        if (!pipelineParams.containsKey("branchingModel")) {
            configurationErrors.add("BranchingModel is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }

        if (!configurationErrors.isEmpty()) {
            error("Found error(s) in the configuration:\n ${configurationErrors.toString()}")
        }
    }

    void setDefaults() {
        //Build flags
        //Use default value, this also creates the key/value pair in the map.
        pipelineParams.get("buildArtifact", false)
        pipelineParams.get("buildDockerImage", true)
        pipelineParams.get("publishArtifact", false)
        pipelineParams.get("publishDockerImage", true)
        pipelineParams.get("isSecurityScanEnabled", true)
        pipelineParams.get("isSonarAnalysisEnabled", true)
        pipelineParams.get("publishStaticAssetsToS3", true)
        pipelineParams.get("pathToSrc", script.env.WORKSPACE)
        pipelineParams.put("branchName", script.env.BRANCH_NAME)
        pipelineParams.get("applicationDependencies", [:])  //list of application dependencies for build

        //TODO: use new newrelic method
        //        this.newRelicId = config.get("newRelicIdMap").get(branchName)
    }

    void collectJobParameters(){
        generate parameters
        jobWithProperties
        private Map generateJobParameters(Map pipelineParams){
            switch (pipelineParams.get("branchName")){

            }
        }
    }

    void configureSlave() {
        //Slave settings
        this.buildContainer = pipelineParams.get("buildContainer")
        if (!buildContainer) {
            configurationErrors.add("BuildContainer is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }
        private Map<String, Map> containerResources() {
            Map<String, Map> containerResources = ["jnlp", JNLP_CONTAINER]


            if (!deployOnly) {
                containerResources.put("buildContainer",buildContainer)
            }
            if (kubernetesDeployment) {
                containerResources.put("kubernetes",KUBERNETES_CONTAINER)
            }
            if (ansibleDeployment) {
                containerResources.put("ansible",ANSIBLE_CONTAINER)
            }
            DOCKER_CONTAINER
            return containerResources
        }
    }

/////add deployment keys
//    kubernetesDeploymentsList = pipelineParams.kubernetesDeploymentsList ?: [APP_NAME]
//    kubernetesClusterMap
//    ansibleRepo
//    ansibleRepoBranch
//    FULL_INVENTORY_PATH
//    BASIC_INVENTORY_PATH
//    DEPLOY_ON_K8S
//    ANSIBLE_DEPLOYMENT
//    kubernetesCluster
//    ANSIBLE_ENV
//    DEPLOY_ENVIRONMENT
//            flow:
//    buildcommants
//    unittestcommands
//    integrationtestcommands
//    testpostcommands
//    postdeploycommands
//    deploy only
//    BUILD_VERSION
//    ANSIBLE_EXTRA_VARS
}


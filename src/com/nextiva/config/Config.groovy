package com.nextiva.config

import static com.nextiva.SharedJobsStaticVars.*

class Config implements Serializable {
    final protected Map pipelineParams
    final protected Map slaveConfig
    final protected script
    private List<String> configurationErrors

    final private String appName
    final private String channelToNotify
    final private String buildTool
    final private enum branchingModel


    protected Config(script, pipelineParams) {
        this.script = script
        this.pipelineParams = pipelineParams


        this.appName = pipelineParams.get("appName")
        if (!appName) {
            configurationErrors.add("Application Name is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }
        this.channelToNotify = config.get("channelToNotify")
        if (!channelToNotify) {
            configurationErrors.add("Slack notification channel is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }

        this.branchName = script.env.BRANCH_NAME


        this.newRelicId = config.get("newRelicIdMap").get(branchName)
        if (!newRelicId) {
            configurationErrors.add("NewRelicId is undefined for branch $branchName. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }


        this.buildTool = config.get("buildTool")
        if (!buildTool) {
            configurationErrors.add("BuildTool is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }

        this.pathToSrc = config.get("pathToSrc", script.env.WORKSPACE)  //if omitted, we always use the $WORKSPACE

        this.branchingModel = config.get("branchingModel")
        if (!branchingModel) {
            configurationErrors.add("BranchingModel is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }

        this.buildContainer = pipelineParams.get("buildContainer")
        if(!buildContainer){
            configurationErrors.add("BuildContainer is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }

        this.healthCheck = config.get("healthCheckMap").get(branchName)
        if (!healthCheck) {
            configurationErrors.add("healthCheck is undefined for this branch. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }



        kubernetesClusterMap
//        ansibleRepo
//        ansibleRepoBranch
//        FULL_INVENTORY_PATH
//        BASIC_INVENTORY_PATH
        DEPLOY_ON_K8S
        ANSIBLE_DEPLOYMENT
        NEWRELIC_APP_ID_MAP
        kubernetesDeploymentsList = pipelineParams.kubernetesDeploymentsList ?: [APP_NAME]
        kubernetesCluster
        ANSIBLE_ENV
        healthCheckUrl
        DEPLOY_ENVIRONMENT
        isSecurityScanEnabled

        flow:
        buildcommants
        unittestcommands
        integrationtestcommands
        testpostcommands
        postdeploycommands



        deploy only
        BUILD_VERSION

        autoincrementversion

        ANSIBLE_EXTRA_VARS

        if (configurationErrors) {
            error("Found error(s) in the configuration:\n ${configurationErrors.toString()}")
        }
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





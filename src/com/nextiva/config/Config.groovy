package com.nextiva.config

import static com.nextiva.SharedJobsStaticVars.*

class Config implements Serializable {
    final protected Map pipelineParams
    final protected Map slaveConfig
    final protected script
    final protected Map jobProperties
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
        this.channelToNotify = pipelineParams.get("channelToNotify")
        if (!channelToNotify) {
            configurationErrors.add("Slack notification channel is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }

        this.buildTool = config.get("buildTool")
        if (!buildTool) {
            configurationErrors.add("BuildTool is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }

        this.buildArtifact = pipelineParams.get("buildArtifact", false)
        this.buildDockerImage = pipelineParams.get("buildDockerImage", true)
        this.publishArtifact = pipelineParams.get("publishArtifact", false)
        this.publishDockerImage = pipelineParams.get("publishDockerImage", true)
        this.isSecurityScanEnabled = pipelineParams.get("isSecurityScanEnabled", true)
        this.isSonarAnalysisEnabled = pipelineParams.get("isSonarAnalysisEnabled", true)
        this.publishStaticAssetsToS3 = pipelineParams.get("publishStaticAssetsToS3", true)
        this.pathToSrc = config.get("pathToSrc", script.env.WORKSPACE)  //if omitted, we always use the $WORKSPACE

        this.applicationDependencies = config.get("applicationDependencies", [:])  //list of application dependencies for build

        this.buildContainer = pipelineParams.get("buildContainer")
        if(!buildContainer){
            configurationErrors.add("BuildContainer is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }



        ansibleDeployment = config.get("ansibleDeployment", false)
        if (ansibleDeployment){
            ansibleDeployment= new AnsibleDeplopyment
            //        ansibleRepo
//        ansibleRepoBranch
//        FULL_INVENTORY_PATH
//        BASIC_INVENTORY_PATH
        }

///////////////////////////with branch////////////////////////////////////////

        this.branchingModel = config.get("branchingModel")
        if (!branchingModel) {
            configurationErrors.add("BranchingModel is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }

        this.branchName = script.env.BRANCH_NAME

        this.newRelicId = config.get("newRelicIdMap").get(branchName)
        if (!newRelicId) {
            configurationErrors.add("NewRelicId is undefined for branch $branchName. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }

        this.healthCheck = config.get("healthCheckMap").get(branchName)
        if (!healthCheck) {
            configurationErrors.add("healthCheck is undefined for this branch. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }


        kubernetesDeploymentsList = pipelineParams.kubernetesDeploymentsList ?: [APP_NAME]

        kubernetesClusterMap
//        ansibleRepo
//        ansibleRepoBranch
//        FULL_INVENTORY_PATH
//        BASIC_INVENTORY_PATH
        DEPLOY_ON_K8S
        ANSIBLE_DEPLOYMENT
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


    private ansibleDeployment(Map configuration){
        return Map = []
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


    private Map setJobProperties(Map pipelineparams){

        jobWithProperties(jobProperties)


    }
}


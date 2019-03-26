package config

class Config implements Serializable {
    final protected Map config
    final protected Map slaveConfig
    final protected script
    private List<String> configurationErrors

    final private String appName
    final private String channelToNotify
    final private String buildTool
    final private enum branchingModel


    protected Config(script, configuration) {
        this.script = script
        this.config = configuration.get("config")
        if (config == null) {
            configurationErrorList.add("Pipeline configuration is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }

        this.branchName = script.env.BRANCH_NAME

        this.appName = config.get("appName")
        if (appName == null) {
            configurationErrorList.add("Application Name is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }

        this.newRelicId = config.get("newRelicIdMap").get(branchName, "sdfdf")
        if (newRelicId == null) {
            configurationErrorList.add("NewRelicId is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }

        this.channelToNotify = config.get("channelToNotify")
        if (channelToNotify == null) {
            configurationErrorList.add("Slack notification channel is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }

        this.buildTool = config.get("buildTool")
        if (buildTool == null) {
            configurationErrorList.add("BuildTool is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }

        this.pathToSrc = config.get("pathToSrc", "$script.env.WORKSPACE")  //if omitted, we always use the $WORKSPACE

        this.branchingModel = config.get("branchingModel")
        if (branchingModel == null) {
            configurationErrorList.add("BranchingModel is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }


def getParamValue(Map source, String paramName, defaultValue){

}

def getMapParamValue(Map source, String param, defaultValue, ){

}


        healthCheckMap
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
}

enum BranchingModel {
    GITFLOW,
    TRUNCKBASED
}
package com.nextiva.config

import java.util.regex.Pattern

import static com.nextiva.SharedJobsStaticVars.*

class Config implements Serializable {
    // used to store all parameters passed into config
    Map configuration = [:]
    private Script script

    Config(Script script, Map pipelineParams) {
        this.script = script
        configuration << pipelineParams
        validate()
        setDefaults()
        setExtraEnvVariables()
        collectJobParameters()
        configureSlave()
    }

    private void validate() {
        // Checking mandatory variables
        List<String> configurationErrors = []

        //TODO: add default appname generation based on the repository name
        if (!configuration.containsKey("appName")) {
            configurationErrors.add("Application Name is undefined. You have to add it in the pipeline  <<LINK_ON_CONFLUENCE>>")
        }
        if (!configuration.containsKey("channelToNotify")) {
            configurationErrors.add("Slack notification channel is undefined. You have to add it in the pipeline  <<LINK_ON_CONFLUENCE>>")
        }
        if (!configuration.containsKey("build")) {
            configurationErrors.add("Build is undefined. You have to add it in the pipeline  <<LINK_ON_CONFLUENCE>>")
        }

        if (!configurationErrors.isEmpty()) {
            script.error("Found error(s) in the configuration:\n ${configurationErrors.toString()}")
        }
    }

    private void setDefaults() {
        //Build flags
        //Use default value, this also creates the key/value pair in the map.
        //TODO: move branching model(gitflow and trunkbased) to the class or enum
        configuration.get("branchingModel", "gitflow")
        configuration.put("isUnitTestEnabled", configuration.test?.containsKey("unitTestCommands"))
        configuration.put("isIntegrationTestEnabled", configuration.test?.containsKey("integrationTestCommands"))
        configuration.put("isDeployEnabled", configuration.containsKey("deploy"))
        configuration.put("isPostDeployEnabled", configuration.deploy?.containsKey("postDeployCommands"))
        configuration.get("isSecurityScanEnabled", true)
        configuration.get("isSonarAnalysisEnabled", true)
        configuration.get("isQACoreTeamTestEnabled", true)
        configuration.get("publishStaticAssetsToS3", true)
        configuration.get("pathToSrc", script.env.WORKSPACE)
        configuration.put("branchName", script.env.BRANCH_NAME)
        configuration.get("extraEnvs", [:])

        //TODO: use new newrelic method
        //        this.newRelicId = config.get("newRelicIdMap").get(branchName)
    }

    private void collectJobParameters() {
        Map properties = generateJobParameters(Map configuration)
        configuration.put("params", jobWithProperties(properties))
    }

    private List generateJobParameters() {
        List paramlist = []
        def branchName = configuration.get("branchName")
        def branchingModel = configuration.get("branchingModel")

        List jobParameters = [["parameter"     : script.string(name: 'deploy_version', defaultValue: '', description: 'Set artifact version for skip all steps and deploy only or leave empty for start full build'),
                               "branchingModel": ["gitflow"   : /^((hotfix|release)\/.+)$/,
                                                  "trunkbased": /^master$/],
                              ],
                              ["parameter"     : script.choice(choices: configuration.get("deployDstList"), description: 'Where deploy?', name: 'deployDst'),
                               "branchingModel": ["gitflow"   : /^((hotfix|release)\/.+)$/,
                                                  "trunkbased": /^master$/],
                              ]]

        jobParameters.each {
            Pattern branchPattern = Pattern.compile(it.get("branchingModel").get(branchingModel))
            if (branchName ==~ branchPattern) {
                paramlist.add(it.get("parameter"))
            }
        }

        return paramlist
    }

    private void setExtraEnvVariables() {
        configuration.extraEnvs.each { k, v -> script.env[k] = v }
    }

    private void configureSlave() {
        //Slave settings
        this.buildContainer = configuration.get("buildContainer")
        if (!buildContainer) {
            configurationErrors.add("BuildContainer is undefined. You have to add it in the commonConfig  <<LINK_ON_CONFLUENCE>>")
        }
        private Map<String, Map> containerResources() {
            Map<String, Map> containerResources = ["jnlp", JNLP_CONTAINER]


            if (!deployOnly) {
                containerResources.put("buildContainer", buildContainer)
            }
            if (kubernetesDeployment) {
                containerResources.put("kubernetes", KUBERNETES_CONTAINER)
            }
            if (ansibleDeployment) {
                containerResources.put("ansible", ANSIBLE_CONTAINER)
            }
            DOCKER_CONTAINER
            return containerResources
        }
    }

    Map getConfiguration() {
        return configuration
    }
}



/////add deployment keys
//    kubernetesDeploymentsList = configuration.kubernetesDeploymentsList ?: [APP_NAME]
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
//    ANSIBLE_EXTRA_VARSrceLimitMemory = containerConfig.get("resourceLimitMemory", "6144Mi")
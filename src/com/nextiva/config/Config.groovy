package com.nextiva.config

import com.nextiva.environment.Environment
import com.nextiva.environment.EnvironmentFactory

import static com.nextiva.SharedJobsStaticVars.*

class Config implements Serializable {
    // used to store all parameters passed into config
    Map configuration = [:]
    private Script script

    Config(Script script, Map pipelineParams) {
        this.script = script
        this.configuration << pipelineParams
        validate()
        setDefaults()
        setExtraEnvVariables()
        setJobParameters()
        configureSlave()
    }

    private void validate() {
        // Checking mandatory variables
        List<String> configurationErrors = []

        //TODO: add default appName generation based on the repository name
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
        //Set flags
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
        configuration.put("branchName", script.env.BRANCH_NAME)
        configuration.get("extraEnvs", [:])

        //TODO: use new newrelic method
        //        this.newRelicId = config.get("newRelicIdMap").get(branchName)
    }

    private void configureEnvironment() {
        if (configuration.get("isDeployEnabled")) {
            EnvironmentFactory environmentFactory = new EnvironmentFactory(configuration)
            List<Environment> environmentsToDeploy =  environmentFactory.getAvailableEnvironmentsForBranch(configuration.get("branchName"))
            configuration.deploy?.put("environmentsToDeploy", environmentsToDeploy)
        }
    }

    private void setJobParameters() {
        JobProperties jobProperties = new JobProperties(script, configuration)
        configuration.put("jobProperties", jobProperties.toMap())
    }

    private void setExtraEnvVariables() {
        configuration.extraEnvs.each { k, v -> script.env[k] = v }
    }

    private void configureSlave() {
        Map build = [:]

        //Slave settings
        this.buildContainer = configuration.get("build").get
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



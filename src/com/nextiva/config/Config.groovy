package com.nextiva.config

import com.cloudbees.groovy.cps.NonCPS
import com.nextiva.environment.Environment
import com.nextiva.environment.EnvironmentFactory
import com.nextiva.stages.StageFactory
import com.nextiva.stages.stage.Stage

class Config implements Serializable {
    // used to store all parameters passed into config
    Map configuration = [:]
    Script script

    Config(Script script, Map pipelineParams) {
        this.script = script
        this.configuration = pipelineParams
    }

    void configure() {
        validate()
        info("preload validate() complete")
        setDefaults()
        info("preload setDefaults() complete")
        configureEnvironment()
        info("preload configureEnvironment()complete")
        setExtraEnvVariables()
        info("preload setExtraEnvVariables() complete")
        setJobParameters()
        info("preload setJobParameters() complete")
        configureStages()
        info("preload configureStages() complete")
        configureSlave()
        info("preload configureSlave() complete")
        info("=================================")
        info("Configuration complete:\n ${toString()}")
    }

    void validate() {
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

    void setDefaults() {
        //Set flags
        //Use default value, this also creates the key/value pair in the map.
        //TODO: move branching model(gitflow and trunkbased) to the class or enum
        configuration.get("branchingModel", "gitflow")
        configuration.get("jobTimeoutMinutes", "60")
        configuration.put("isUnitTestEnabled", configuration.build.any { buildTool, toolConfiguration ->
            toolConfiguration.containsKey("unitTestCommands")
        })
        configuration.put("isIntegrationTestEnabled", configuration.build.any { buildTool, toolConfiguration ->
            toolConfiguration.containsKey("integrationTestCommands")
        })
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

    void configureEnvironment() {
        if (configuration.get("isDeployEnabled")) {
            EnvironmentFactory environmentFactory = new EnvironmentFactory(configuration)
            List<Environment> environmentsToDeploy = environmentFactory.getAvailableEnvironmentsForBranch(configuration.get("branchName"))
            configuration.put("environmentsToDeploy", environmentsToDeploy)
        }
    }

    void setExtraEnvVariables() {
        configuration.extraEnvs.each { k, v -> script.env[k] = v }
    }

    void setJobParameters() {
        JobProperties jobProperties = new JobProperties(script, configuration)
        def props = jobProperties.getParams()
        configuration.put("jobProperties", props)
    }

    void configureSlave() {
        SlaveFactory slaveFactory = new SlaveFactory(script, configuration)
        def slaveConfiguration = slaveFactory.getSlaveConfiguration()
        configuration.put("slaveConfiguration", slaveConfiguration)
    }

    void configureStages() {
        List<Stage> stages = StageFactory.getStagesFromConfiguration(script, configuration)
        echo("selected stages")
        stages.each { echo(it.getClass().getSimpleName()) }

        configuration.put("stages", stages)
    }

    Map getConfiguration() {
        return configuration
    }

    Map getSlaveConfiguration() {
        return configuration.get("slaveConfiguration")
    }

    String getJobTimeoutMinutes() {
        return configuration.get("jobTimeoutMinutes")
    }

    List<Stage> getStages() {
        def stages = configuration.get("stages")
        echo("stages:$stages")
        return stages
    }

    @NonCPS
    protected info(msg) {
        script.log.info("[${this.getClass().getSimpleName()}] ${msg}")
    }

    @NonCPS
    protected debug(msg) {
        script.log.debug("[${this.getClass().getSimpleName()}] ${msg}")
    }

    @Override
    String toString() {
        String toString = ''
        configuration.each { k, v ->
            toString += "[$k]=$v\n"
        }
        return toString
    }

}



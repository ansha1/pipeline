package com.nextiva.config


import com.nextiva.environment.Environment
import com.nextiva.environment.EnvironmentFactory
import com.nextiva.stages.StageFactory
import com.nextiva.stages.stage.Stage
import com.nextiva.utils.Logger
import hudson.AbortException

class Config implements Serializable {
    // used to store all parameters passed into config
    Map configuration = [:]
    Script script
    Logger log = new Logger(this)

    Config(Script script, Map pipelineParams) {
        this.script = script
        this.configuration = pipelineParams
    }

    void configure() {
        log.debug("start job configuration")
        validate()
        setDefaults()
        setJobParameters()
        setExtraEnvVariables()
        initBuildTools()
        configureDeployEnvironment()
        initDeployTools()
        configureStages()
        configureSlave()
        log.debug("Configuration complete:", configuration)
    }

    void validate() {
        log.debug("start validate()")
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
            log.error("Found error(s) in the configuration:", configurationErrors)
            throw new AbortException("errors in the configuration")
        }
        log.debug("complete validate()")
    }

    void setJobParameters() {
        log.debug("start setJobParameters()")
        JobProperties jobProperties = new JobProperties(script, configuration)
        def props = jobProperties.getParams()
        log.debug("Job properties", props)
        configuration.put("jobProperties", props)


        log.debug("Chosen deploy version", props.deployVersion)
        def deployOnly = false
        if (props.deployVersion) {
            deployOnly = true
        }
        log.debug("set deployOnly: $deployOnly")
        configuration.put("deployOnly", deployOnly)

        log.debug("complete setJobParameters()")
    }

    void setDefaults() {
        log.debug("start setDefaults()")
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
        configuration.get("isJobHasDependencies", configuration.containsKey("dependencies"))
        configuration.get("isSecurityScanEnabled", true)
        configuration.get("isSonarAnalysisEnabled", true)
        configuration.get("isQACoreTeamTestEnabled", true)
        configuration.put("branchName", script.env.BRANCH_NAME)
        configuration.get("extraEnvs", [:])

        //TODO: use new newrelic method
        //        this.newRelicId = config.get("newRelicIdMap").get(branchName)
        log.debug("complete setDefaults()")
    }

    void configureDeployEnvironment() {
        log.debug("start configureDeployEnvironment()")
        if (configuration.get("isDeployEnabled")) {
            EnvironmentFactory environmentFactory = new EnvironmentFactory(configuration)
            List<Environment> environmentsToDeploy = environmentFactory.getAvailableEnvironmentsForBranch(configuration.get("branchName"))
            configuration.put("environmentsToDeploy", environmentsToDeploy)
        }
        log.debug("complete configureDeployEnvironment()")
    }

    void setExtraEnvVariables() {
        log.debug("start setExtraEnvVariables() complete")
        configuration.extraEnvs.each { k, v ->
            log.debug("[$k]=$v")
            script.env[k] = v
        }
        log.debug("complete setExtraEnvVariables() complete")
    }

    void configureStages() {
        log.debug("start configureStages()")
        StageFactory stageFactory = new StageFactory(script, configuration)
        List<Stage> stages = stageFactory.getStagesFromConfiguration()
        log.debug("Selected stages:", stages)
        configuration.put("stages", stages)
        log.debug("complete configureStages()")
    }

    void configureSlave() {
        log.debug("start configureSlave()")
        SlaveFactory slaveFactory = new SlaveFactory(script, configuration)
        def slaveConfiguration = slaveFactory.getSlaveConfiguration()
        log.debug("slave configuration:", slaveConfiguration)
        configuration.put("slaveConfiguration", slaveConfiguration)
        log.debug("complete configureSlave()")
    }

    void initBuildTools(){
        log.debug("start initBuildTools()")
        log.complete("start initBuildTools()")
    }
    void initDeployTools(){
        log.debug("start initDepoyTools()")
        log.complete("start initDepoyTools()")
    }

    Map getConfiguration() {
        log.debug("returning configuration ", configuration)
        return configuration
    }

    Map getSlaveConfiguration() {
        Map slaveConfiguration = configuration.get("slaveConfiguration")
        log.debug("returning slave configuration ", slaveConfiguration)
        return slaveConfiguration
    }

    String getJobTimeoutMinutes() {
        return configuration.get("jobTimeoutMinutes")
    }

    List<Stage> getStages() {
        List stages = configuration.get("stages")
        log.debug("Pipeline stages: \n", stages)
        return stages
    }
}



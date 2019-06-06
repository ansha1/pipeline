package com.nextiva.config

import com.cloudbees.groovy.cps.NonCPS
import com.nextiva.environment.Environment
import com.nextiva.environment.EnvironmentFactory
import com.nextiva.stages.StageFactory
import com.nextiva.stages.stage.Stage
import com.nextiva.utils.Logger

import static groovy.json.JsonOutput.*

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
        configureEnvironment()
        setExtraEnvVariables()
        setJobParameters()
        configureStages()
        configureSlave()
        log.debug("Configuration complete:\n", configuration)
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
            script.error("Found error(s) in the configuration:\n ${configurationErrors.toString()}")
        }
        log.debug("complete validate()")
    }

    void setJobParameters() {
        log.debug("start setJobParameters()")
        JobProperties jobProperties = new JobProperties(script, configuration)
        def props = jobProperties.getParams()
        log.debug("Job properties", props)
        configuration.put("jobProperties", props)

        //TODO: move this into the proper place
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
        configuration.put("isPostDeployEnabled", configuration.deploy?.containsKey("postDeployCommands"))
        configuration.get("isSecurityScanEnabled", true)
        configuration.get("isSonarAnalysisEnabled", true)
        configuration.get("isQACoreTeamTestEnabled", true)
        configuration.put("branchName", script.env.BRANCH_NAME)
        configuration.get("extraEnvs", [:])

        //TODO: use new newrelic method
        //        this.newRelicId = config.get("newRelicIdMap").get(branchName)
        log.debug("complete setDefaults()")
    }

    void configureEnvironment() {
        log.debug("start configureEnvironment()")
        if (configuration.get("isDeployEnabled")) {
            EnvironmentFactory environmentFactory = new EnvironmentFactory(configuration)
            List<Environment> environmentsToDeploy = environmentFactory.getAvailableEnvironmentsForBranch(configuration.get("branchName"))
            configuration.put("environmentsToDeploy", environmentsToDeploy)
        }
        log.debug("complete configureEnvironment()")
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



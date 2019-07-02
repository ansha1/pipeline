package com.nextiva.config


import com.nextiva.environment.Environment
import com.nextiva.environment.EnvironmentFactory
import com.nextiva.stages.StageFactory
import com.nextiva.stages.stage.Stage
import com.nextiva.tools.Tool
import com.nextiva.tools.ToolFactory
import com.nextiva.utils.Logger
import hudson.AbortException

import static com.nextiva.utils.Utils.getGlobal
import static com.nextiva.utils.Utils.setGlobalVersion

class Config implements Serializable {
    // used to store all parameters passed into config
    Map configuration = [:]
    Script script
    ToolFactory toolFactory = new ToolFactory()
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
        configureSlave()
        setExtraEnvVariables()
        configureDependencyProvisioning()
        configureBuildTools()
        configureDeployTools()
        configureDeployEnvironment()
        configureStages()
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
        if (!configurationErrors.isEmpty()) {
            log.error("Found error(s) in the configuration:", configurationErrors)
            throw new AbortException("errors in the configuration")
        }
        log.debug("complete validate()")
    }

    void setDefaults() {
        log.debug("start setDefaults()")
        //Set flags
        //Use default value, this also creates the key/value pair in the map.
        Global global = getGlobal()
        String appName = configuration.get("appName")
        global.setAppName(appName)
        //TODO: move branching model(gitflow and trunkbased) to the class or enum
        String branchingModel = configuration.get("branchingModel", "gitflow")
        global.setBranchingModel(branchingModel)
        configuration.put("branchName", script.env.BRANCH_NAME)
        global.setBranchName(script.env.BRANCH_NAME)
        configuration.get("jobTimeoutMinutes", "60")
        configuration.put("isUnitTestEnabled", configuration.build.any { buildTool, toolConfiguration ->
            toolConfiguration.containsKey("unitTestCommands")
        })
        configuration.put("isIntegrationTestEnabled", configuration.build.any { buildTool, toolConfiguration ->
            toolConfiguration.containsKey("integrationTestCommands")
        })
        configuration.get("isSecurityScanEnabled", true)
        configuration.get("isSonarAnalysisEnabled", true)
        configuration.get("isQACoreTeamTestEnabled", true)

        //TODO: use new newrelic method
        //        this.newRelicId = config.get("newRelicIdMap").get(branchName)
        log.debug("complete setDefaults()")
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
            log.info("Deploy version ${props.deployVersion} has been setted by job parameters \n set it as globalVersion.")
            setGlobalVersion(props.deployVersion)
            deployOnly = true
        }
        log.debug("set deployOnly: $deployOnly")
        configuration.put("deployOnly", deployOnly)

        log.debug("complete setJobParameters()")
    }

    void configureSlave() {
        log.debug("start configureSlave()")
        Map containerResources = [:]
        Map jenkinsContainer = configuration.get("jenkinsContainer", ["name": "jnlp"])
        toolFactory.mergeWithDefaults(jenkinsContainer)
        log.debug("added jenkins container")
        containerResources.put("jnlp", jenkinsContainer)
        Map slaveConfiguration = ["containerResources": containerResources]
        log.debug("slave configuration:", slaveConfiguration)
        configuration.put("slaveConfiguration", slaveConfiguration)
        log.debug("complete configureSlave()")
    }

    void setExtraEnvVariables() {
        log.debug("start setExtraEnvVariables() complete")
        Map extraEnvs = configuration.get("extraEnvs")
        if (extraEnvs != null) {
            extraEnvs.each { k, v ->
                log.debug("[$k]=$v")
                script.env[k] = v
            }
        }
        log.debug("complete setExtraEnvVariables() complete")
    }

    void configureDependencyProvisioning() {
        log.debug("start configuring build dependency provisioning")
        Boolean isJobHasDependencies = false
        if (configuration.containsKey("dependencies")) {
            Map kubeup = ["name": "kubeup"]
            toolFactory.mergeWithDefaults(kubeup)
            putSlaveContainerResource("kubeup", kubeup)
            isJobHasDependencies = true
        }
        configuration.put("isJobHasDependencies", isJobHasDependencies)
        log.debug("complete configuring build dependency provisioning")
    }

    void configureBuildTools() {
        log.debug("start configureBuildTools()")
        Map<String, Map> buildTools = configuration.get("build")
        if (buildTools == null) {
            log.error("Build is undefined. You have to add it in the pipeline  <<LINK_ON_CONFLUENCE>>")
            throw new AbortException("Build is undefined. You have to add it in the pipeline  <<LINK_ON_CONFLUENCE>>")
        }

        buildTools.each { tool, toolConfig ->
            log.debug("got build tool $tool")
            toolConfig.put("name", tool)
            toolFactory.mergeWithDefaults(toolConfig)
            putSlaveContainerResource(tool, toolConfig)
            Tool instance = toolFactory.build(script, toolConfig)
            toolConfig.put("instance", instance)
        }
        log.trace("Built tools after configuring:${buildTools.toString()}")
        log.debug("complete configureBuildTools()")
    }

    void configureDeployTools() {
        log.debug("start configureDeployTools()")
        String deployTool = configuration.get("deployTool")
        if (deployTool != null) {
            Map deploy = [:]
            deploy.put(deployTool, [:])
            configuration.put("deploy", deploy)
        } else {
            log.debug("deployTool is undefined")
        }

        Map<String, Map> deployTools = configuration.get("deploy")
        if (deployTools != null) {
            deployTools.each { tool, toolConfig ->
                log.debug("got deploy tool $tool")
                toolConfig.put("name", tool)
                toolFactory.mergeWithDefaults(toolConfig)
                putSlaveContainerResource(tool, toolConfig)
                Tool instance = toolFactory.build(script, toolConfig)
                toolConfig.put("instance", instance)
            }
            log.trace("Deploy tools after configuring:${deployTools.toString()}")
            configuration.put("isDeployEnabled", true)
        } else {
            log.info("Deploy tools is undefined isDeployEnabled=false")
            configuration.put("isDeployEnabled", false)
        }
        log.debug("complete configureDeployTool()")
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

    void configureStages() {
        log.debug("start configureStages()")
        StageFactory stageFactory = new StageFactory(script, configuration)
        List<Stage> stages = stageFactory.getStagesFromConfiguration()
        log.debug("Selected stages:", stages)
        configuration.put("stages", stages)
        log.debug("complete configureStages()")
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

    Boolean putSlaveContainerResource(String name, Map containerResource) {
        return configuration.slaveConfiguration.containerResources.put(name, containerResource)
    }

    List<Stage> getStages() {
        List stages = configuration.get("stages")
        log.debug("Pipeline stages: \n", stages)
        return stages
    }
}



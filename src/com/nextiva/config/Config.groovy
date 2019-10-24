package com.nextiva.config


import com.nextiva.environment.EnvironmentFactory
import com.nextiva.stages.StageFactory
import com.nextiva.stages.stage.Stage
import com.nextiva.tools.Tool
import com.nextiva.tools.ToolFactory
import com.nextiva.tools.deploy.DeployTool
import com.nextiva.utils.Logger
import hudson.AbortException

import static com.nextiva.config.Global.instance as global
import static com.nextiva.utils.Utils.setGlobalVersion

class Config implements Serializable {
    // used to store all parameters passed into config
    Map configuration = [:]
    Script script
    ToolFactory toolFactory = new ToolFactory()
    Logger logger = new Logger(this)

    Config(Script script, Map pipelineParams) {
        this.script = script
        this.configuration = pipelineParams
    }

    void configure() {
        logger.debug("start job configuration")
        validate()
        setDefaults()
        setJobParameters()
        configureSlave()
        setExtraEnvVariables()
        configureDependencyProvisioning()
        configureBuildTools()
        configureDeployTool()
        configureDeployEnvironment()
        configureStages()
        logger.debug("Configuration complete:", configuration)
    }

    void validate() {
        logger.debug("start validate()")
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
            logger.error("Found error(s) in the configuration:", configurationErrors)
            throw new AbortException("errors in the configuration")
        }
        logger.debug("complete validate()")
    }

    void setDefaults() {
        logger.debug("start setDefaults()")
        //Set flags
        //Use default value, this also creates the key/value pair in the map.
        Global global = getGlobal()
        global.script = script
        String appName = configuration.get("appName")
        global.setAppName(appName)
        //TODO: move branching model(gitflow and trunkbased) to the class or enum
        String branchingModel = configuration.get("branchingModel", "gitflow")
        global.setBranchingModel(branchingModel)
        configuration.put("branchName", script.env.BRANCH_NAME)
        global.setBranchName(script.env.BRANCH_NAME)
        configuration.get("jobTimeoutMinutes", "60")
        configuration.get("isUnitTestEnabled", true)
        configuration.put("isIntegrationTestEnabled", configuration.build.any { buildTool, toolConfiguration ->
            toolConfiguration.containsKey("integrationTestCommands")
        })
        configuration.get("isSecurityScanEnabled", true)
        configuration.get("isSonarAnalysisEnabled", true)
        configuration.get("isQACoreTeamTestEnabled", true)

        //TODO: use new newrelic method
        //        this.newRelicId = config.get("newRelicIdMap").get(branchName)
        logger.debug("complete setDefaults()")
    }

    void setJobParameters() {
        logger.debug("start setJobParameters()")
        JobProperties jobProperties = new JobProperties(script, configuration)
        def props = jobProperties.getParams()
        logger.debug("Job properties", props)
        configuration.put("jobProperties", props)

        logger.debug("Chosen deploy version", props.deployVersion)
        def deployOnly = false
        if (props.deployVersion) {
            logger.info("Deploy version ${props.deployVersion} has been setted by job parameters \n set it as globalVersion.")
            global.globalVersion = props.deployVersion
            deployOnly = true
        }
        logger.debug("set deployOnly: $deployOnly")
        configuration.put("deployOnly", deployOnly)

        logger.debug("complete setJobParameters()")
    }

    void configureSlave() {
        logger.debug("start configureSlave()")
        Map containerResources = [:]
        Map jenkinsContainer = configuration.get("jenkinsContainer", ["name": "jnlp"])
        toolFactory.mergeWithDefaults(jenkinsContainer)
        logger.debug("added jenkins container")
        containerResources.put("jnlp", jenkinsContainer)
        Map slaveConfiguration = ["containerResources": containerResources,
                                  "rawYaml"           : """\
                                      spec:
                                        tolerations:
                                        - key: tooling.nextiva.io
                                          operator: Equal
                                          value: jenkins
                                          effect: NoSchedule
                                  """.stripIndent()]
        logger.debug("slave configuration:", slaveConfiguration)
        configuration.put("slaveConfiguration", slaveConfiguration)
        logger.debug("complete configureSlave()")
    }

    void setExtraEnvVariables() {
        logger.debug("start setExtraEnvVariables() complete")
        Map extraEnvs = configuration.get("extraEnvs")
        if (extraEnvs != null) {
            extraEnvs.each { k, v ->
                logger.debug("[$k]=$v")
                script.env[k] = v
            }
        }
        logger.debug("complete setExtraEnvVariables() complete")
    }

    void configureDependencyProvisioning() {
        logger.debug("start configuring build dependency provisioning")
        Boolean isJobHasDependencies = false
        if (configuration.containsKey("dependencies")) {
            Map kubeup = ["name": "kubeup"]
            toolFactory.mergeWithDefaults(kubeup)
            putSlaveContainerResource("kubeup", kubeup)
            isJobHasDependencies = true
        }
        configuration.put("isJobHasDependencies", isJobHasDependencies)
        logger.debug("complete configuring build dependency provisioning")
    }

    void configureBuildTools() {
        logger.debug("start configureBuildTools()")
        Map<String, Map> buildTools = configuration.get("build")
        if (buildTools == null) {
            logger.error("Build is undefined. You have to add it in the pipeline  <<LINK_ON_CONFLUENCE>>")
            throw new AbortException("Build is undefined. You have to add it in the pipeline  <<LINK_ON_CONFLUENCE>>")
        }

        buildTools.each { tool, toolConfig ->
            logger.debug("got build tool $tool")
            toolConfig.put("name", tool)
            toolFactory.mergeWithDefaults(toolConfig)
            putSlaveContainerResource(tool, toolConfig)
            Tool instance = toolFactory.build(script, toolConfig)
            toolConfig.put("instance", instance)
        }
        logger.trace("Built tools after configuring:${buildTools.toString()}")
        logger.debug("complete configureBuildTools()")
    }

    void configureDeployTool() {
        logger.debug("start configureDeployTools()")

        if (configuration.get("isDeployEnabled", true)) {
            global.isDeployEnabled = true

            String toolName = configuration.get("deployTool", "kubeup")
            logger.debug("Deploy tool is $toolName")

            def toolConfig = ["name": toolName]
            toolFactory.mergeWithDefaults(toolConfig)
            putSlaveContainerResource(toolName, toolConfig)

            DeployTool tool = toolFactory.build(script, toolConfig)
            global.deployTool = tool

            logger.trace("Deploy tool after configuration: ${tool.toString()}")
        } else {
            logger.info("'isDeployEnabled' set to false. Deployment will be skipped.")
            global.isDeployEnabled = false
        }
        logger.debug("complete configureDeployTool()")
    }

    void configureDeployEnvironment() {
        logger.debug("start configureDeployEnvironment()")
        if (global.isDeployEnabled) {
            EnvironmentFactory environmentFactory = new EnvironmentFactory(configuration)
            global.environmentsToDeploy = environmentFactory.getAvailableEnvironmentsForBranch(global.branchName, global.branchingModel)
            configuration.put("environmentsToDeploy", global.environmentsToDeploy)
            logger.trace("Environments to deploy:", global.environmentsToDeploy)
            if (global.environmentsToDeploy.isEmpty()) {
                logger.debug("environmentsToDeploy is empty. Deployment stage be skipped.")
                global.isDeployEnabled = false
            }
        }
        logger.debug("complete configureDeployEnvironment()")
    }

    void configureStages() {
        logger.debug("start configureStages()")
        StageFactory stageFactory = new StageFactory(script, configuration)
        List<Stage> stages = stageFactory.getStagesFromConfiguration()
        logger.debug("Selected stages:", stages)
        configuration.put("stages", stages)
        logger.debug("complete configureStages()")
    }


    Map getConfiguration() {
        logger.debug("returning configuration ", configuration)
        return configuration
    }

    Map getSlaveConfiguration() {
        Map slaveConfiguration = configuration.get("slaveConfiguration")
        logger.debug("returning slave configuration ", slaveConfiguration)
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
        logger.debug("Pipeline stages: \n", stages)
        return stages
    }
}



package com.nextiva.config

import com.cloudbees.groovy.cps.NonCPS
import com.nextiva.environment.Environment
import com.nextiva.environment.EnvironmentFactory
import com.nextiva.stages.StageFactory
import com.nextiva.stages.stage.Stage
import com.nextiva.tools.Tool
import com.nextiva.tools.ToolFactory
import com.nextiva.tools.deploy.DeployTool
import com.nextiva.utils.Logger
import groovy.transform.PackageScope
import hudson.AbortException

import static com.nextiva.SharedJobsStaticVars.PIP_EXTRA_INDEX_URL_DEV
import static com.nextiva.SharedJobsStaticVars.PIP_EXTRA_INDEX_URL_PROD
import static com.nextiva.SharedJobsStaticVars.TWINE_REPOSITORY_URL_DEV
import static com.nextiva.SharedJobsStaticVars.TWINE_REPOSITORY_URL_PROD


/**
 * <p>Globally accessible pipeline configuration singleton.</p>
 *
 * <p>Most properties are copied from {@link com.nextiva.config.PipelineConfig} as a delegate in nexivaPipeline.</p>
 *
 * <p>P.S. For unknown reason {@link Singleton} annotation did not work well when running on Jenkins.</p>
 *
 * @see com.nextiva.config.PipelineConfig
 */
class Config implements Serializable {
    private Script script
    private String appName //TODO: add default appName generation based on the repository name
    private String channelToNotify
    String version
    private String branchName
    private BranchingModel branchingModel
    private String deployToolName
    private Tool deployTool
    private Boolean isDeployEnabled = true
    List<Environment> environmentsToDeploy
    private String jobTimeoutMinutes
    private Boolean isUnitTestEnabled = true
    private Boolean isSecurityScanEnabled = true
    private Boolean isSonarAnalysisEnabled = true
    private Boolean isQACoreTeamTestEnabled = true
    private Boolean isIntegrationTestEnabled = false
    List<Map> build
    private List jobTriggers
    private String buildDaysToKeep
    private String buildNumToKeep
    private String buildArtifactDaysToKeep
    private String buildArtifactNumToKeep
    private Map auth
    private Map jobProperties
    private Boolean deployOnly
    Map jenkinsContainer
    Map slaveConfiguration
    Map<String, String> extraEnvs
    Boolean isJobHasDependencies = false
    Map<String, String> dependencies
    Map<String, String> kubeupConfig
    List<Environment> environments
    DeploymentType deploymentType

    private ToolFactory toolFactory = new ToolFactory()
    private Logger logger = new Logger(this)
    private List<Stage> stages

    // ===== Singleton Details ========================================================================================
    private static Config singleInstance = null

    void setSingleInstance(Config newSingleInstance) {
        this.@singleInstance = newSingleInstance
    }

    private Config() {}

    static Config getInstance() {
        if (singleInstance == null) {
            singleInstance = new Config()
        }
        return singleInstance
    }
    // ================================================================================================================

    void configure(PipelineConfig pipelineConfig) {
        logger.debug("start job configuration")
        copyProperties(pipelineConfig)
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
        this.logger.debug("Configuration complete:", this.toString())
    }


    void copyProperties(PipelineConfig pipelineConfig) {
        this.@script = pipelineConfig.script
        this.@appName = pipelineConfig.appName
        this.@channelToNotify = pipelineConfig.channelToNotify
        this.@version = pipelineConfig.version
        this.@branchName = pipelineConfig.branchName
        this.setBranchingModel(pipelineConfig.branchingModel)
        this.@logger.trace("Branching model is ${this.@branchingModel.class.simpleName}")
        this.@isDeployEnabled = pipelineConfig.isDeployEnabled
        this.@deployToolName = pipelineConfig.deployTool
        this.@jobTimeoutMinutes = pipelineConfig.jobTimeoutMinutes
        this.@isUnitTestEnabled = pipelineConfig.isUnitTestEnabled
        this.@isSecurityScanEnabled = pipelineConfig.isSecurityScanEnabled
        this.@isSonarAnalysisEnabled = pipelineConfig.isSonarAnalysisEnabled
        this.@isQACoreTeamTestEnabled = pipelineConfig.isQACoreTeamTestEnabled
        this.@isIntegrationTestEnabled = pipelineConfig.isIntegrationTestEnabled
        this.@build = pipelineConfig.build
        this.@jobTriggers = pipelineConfig.jobTriggers
        this.@buildDaysToKeep = pipelineConfig.buildDaysToKeep
        this.@buildNumToKeep = pipelineConfig.buildNumToKeep
        this.@buildArtifactDaysToKeep = pipelineConfig.buildArtifactDaysToKeep
        this.@buildArtifactNumToKeep = pipelineConfig.buildArtifactNumToKeep
        this.@auth = pipelineConfig.auth
        this.@jobProperties = pipelineConfig.jobProperties
        this.@deployOnly = pipelineConfig.deployOnly
        this.@jenkinsContainer = pipelineConfig.jenkinsContainer
        this.@slaveConfiguration = pipelineConfig.slaveConfiguration
        this.@extraEnvs = pipelineConfig.extraEnvs
        this.@dependencies = pipelineConfig.dependencies
        this.@kubeupConfig = pipelineConfig.kubeupConfig
        environments = pipelineConfig.environments.collect { it as Environment }
    }

    /**
     * Checking mandatory variables
     */
    @PackageScope
    void validate() {
        logger.debug("start validate()")
        List<String> configurationErrors = []

        if (!appName) {
            configurationErrors.add("Application Name is undefined. You have to add it in the pipeline  <<LINK_ON_CONFLUENCE>>")
        }
        if (!channelToNotify) {
            configurationErrors.add("Slack notification channel is undefined. You have to add it in the pipeline  <<LINK_ON_CONFLUENCE>>")
        }
        if (!build) {
            logger.error("Build is undefined. You have to add it in the pipeline  <<LINK_ON_CONFLUENCE>>")
            throw new AbortException("Build is undefined. You have to add it in the pipeline  <<LINK_ON_CONFLUENCE>>")
        }

        if (!configurationErrors.isEmpty()) {
            logger.error("Found error(s) in the configuration:", configurationErrors)
            throw new AbortException("errors in the configuration")
        }

        logger.debug("complete validate()")
    }

    /**
     * Set some fields to their default values
     */
    @PackageScope
    void setDefaults() {
        logger.debug("start setDefaults()")
        branchName = script.env.BRANCH_NAME

        if (deploymentType == null) {
            if ([GitFlow.feature, TrunkBased.feature].contains(branchingModel.getBranchType(branchName))) {
                deploymentType = DeploymentType.DEV
            } else {
                deploymentType = DeploymentType.RELEASE
            }
        }
        logger.trace("deploymentType", deploymentType)

        isIntegrationTestEnabled = build.any { toolConfiguration ->
            toolConfiguration.containsKey("integrationTestCommands")
        }

        //TODO: use new newrelic method
        // this.newRelicId = config.get("newRelicIdMap").get(branchName)
        logger.debug("complete setDefaults()")
    }

    @PackageScope
    void setJobParameters() {
        logger.debug("start setJobParameters()")
        JobProperties jobProperties = new JobProperties(this)
        def props = jobProperties.getParams()
        logger.debug("Job properties", props)
        this.jobProperties = props

        logger.debug("Chosen deploy version", props.deployVersion)
        if (props.deployVersion) {
            logger.info("Deploy version ${props.deployVersion} has been setted by job parameters \n set it as globalVersion.")
            version = props.deployVersion
            deployOnly = true
        }
        logger.debug("set deployOnly: $deployOnly")
        logger.debug("complete setJobParameters()")
    }

    @PackageScope
    void configureSlave() {
        logger.debug("start configureSlave()")
        Map containerResources = [:]
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
        this.slaveConfiguration = slaveConfiguration
        logger.debug("complete configureSlave()")
    }

    @NonCPS
    @PackageScope
    void setExtraEnvVariables() {
        logger.debug("start setExtraEnvVariables() complete")
        this.@script.env['PIP_INDEX_URL'] = (deploymentType == DeploymentType.DEV) ? PIP_EXTRA_INDEX_URL_DEV :
                PIP_EXTRA_INDEX_URL_PROD
        this.@script.env['TWINE_REPOSITORY_URL'] = (deploymentType == DeploymentType.DEV) ? TWINE_REPOSITORY_URL_DEV :
                TWINE_REPOSITORY_URL_PROD
        if (extraEnvs) {
            logger.debug("Adding extra envVars")
            extraEnvs.each { k, v ->
                logger.debug("[$k]=$v")
                this.@script.env[k] = v
            }
        }
        logger.debug("complete setExtraEnvVariables() complete")
    }

    @PackageScope
    void configureDependencyProvisioning() {
        logger.debug("start configuring build dependency provisioning")
        if (dependencies) {
            logger.debug("Job has dependencies, configuraitng kubeup")
            Map kubeup = ["name": "kubeup"]
            toolFactory.mergeWithDefaults(kubeup)
            //TODO get rid of map put
            putSlaveContainerResource("kubeup", kubeup)
            isJobHasDependencies = true
        }
        logger.debug("complete configuring build dependency provisioning")
    }

    @PackageScope
    void configureBuildTools() {
        logger.debug("start configureBuildTools()")
        build.each { toolConfig ->
            logger.debug("got build tool $toolConfig")
            toolFactory.mergeWithDefaults(toolConfig)
            putSlaveContainerResource(toolConfig.name, toolConfig)
            Tool instance = toolFactory.build(toolConfig)
            // TODO get rid of map put
            toolConfig.put("instance", instance)
        }
        logger.trace("Built tools after configuring: ${build.toString()}")
        logger.debug("complete configureBuildTools()")
    }

    @PackageScope
    void configureDeployTool() {
        logger.debug("start configureDeployTools()")

        if (isDeployEnabled) {
            logger.debug("Deploy tool is $deployToolName")

            def toolConfig = ["name": deployToolName]
            toolFactory.mergeWithDefaults(toolConfig)
            putSlaveContainerResource(deployToolName, toolConfig)

            deployTool = toolFactory.build(toolConfig)

            logger.trace("Deploy tool after configuration: ${deployTool.toString()}")
        } else {
            logger.info("'isDeployEnabled' set to false. Deployment will be skipped.")
        }
        logger.debug("complete configureDeployTool()")
    }

    @PackageScope
    void configureDeployEnvironment() {
        logger.debug("start configureDeployEnvironment()")
        if (isDeployEnabled) {
            logger.trace("Getting EnvironmentFactory($branchingModel, $environments)")
            EnvironmentFactory environmentFactory = new EnvironmentFactory(environments)
            logger.trace("Starting environmentFactory.getAvailableEnvironmentsForBranch($branchingModel, $branchName)")
            environmentsToDeploy = environmentFactory.getAvailableEnvironmentsForBranch(branchingModel, branchName)
            logger.trace("Environments to deploy: ", environmentsToDeploy.collect { it.name })
            if (!environmentsToDeploy) {
                logger.info("No environments to deploy. Deployment stage will be skipped.")
                isDeployEnabled = false
            }
        }
        logger.debug("complete configureDeployEnvironment()")
    }

    @PackageScope
    void configureStages() {
        logger.debug("start configureStages()")
        StageFactory stageFactory = new StageFactory()
        stages = stageFactory.getStagesFromConfiguration()
        logger.debug("complete configureStages()")
    }

    private Boolean putSlaveContainerResource(String name, Map containerResource) {
        return slaveConfiguration.containerResources.put(name, containerResource)
    }

    // ===== Getters and setters =======================================================================================
    @NonCPS
    BranchingModel getBranchingModel() {
        return this.@branchingModel
    }

    @NonCPS
    private void setBranchingModel(String branchingModelName) {
        try {
            BranchingModel model
            switch (branchingModelName.toLowerCase().replaceAll("\\s", "")) {
                case "gitflow":
                    model = new GitFlow()
                    break
                case "trunkbased":
                    model = new TrunkBased()
                    break
                default:
                    throw new AbortException("Supported branching models: 'GitFlow' and 'TrunkBased'. " +
                            "Yours is '$branchingModelName'")
            }
            this.@branchingModel = model
        } catch (Exception e) {
            logger.error(e.message)
            throw e
        }
    }

    @NonCPS
    Tool getDeployTool() {
        return this.@deployTool
    }

    @NonCPS
    Script getScript() {
        return this.@script
    }

    @NonCPS
    String getAppName() {
        return this.@appName
    }

    @NonCPS
    String getChannelToNotify() {
        return this.@channelToNotify
    }

    @NonCPS
    String getBranchName() {
        return this.@branchName
    }

    @NonCPS
    Boolean getIsDeployEnabled() {
        return this.@isDeployEnabled
    }

    @NonCPS
    List<Environment> getEnvironmentsToDeploy() {
        return this.@environmentsToDeploy
    }

    @NonCPS
    String getJobTimeoutMinutes() {
        return this.@jobTimeoutMinutes
    }

    @NonCPS
    Boolean getIsUnitTestEnabled() {
        return this.@isUnitTestEnabled
    }

    @NonCPS
    Boolean getIsSecurityScanEnabled() {
        return this.@isSecurityScanEnabled
    }

    @NonCPS
    Boolean getIsSonarAnalysisEnabled() {
        return this.@isSonarAnalysisEnabled
    }

    @NonCPS
    Boolean getIsQACoreTeamTestEnabled() {
        return this.@isQACoreTeamTestEnabled
    }

    @NonCPS
    Boolean getIsIntegrationTestEnabled() {
        return this.@isIntegrationTestEnabled
    }

    @NonCPS
    List<Map> getBuild() {
        return this.@build
    }

    @NonCPS
    List getJobTriggers() {
        return this.@jobTriggers
    }

    @NonCPS
    String getBuildDaysToKeep() {
        return this.@buildDaysToKeep
    }

    @NonCPS
    String getBuildNumToKeep() {
        return this.@buildNumToKeep
    }

    @NonCPS
    String getBuildArtifactDaysToKeep() {
        return this.@buildArtifactDaysToKeep
    }

    @NonCPS
    String getBuildArtifactNumToKeep() {
        return this.@buildArtifactNumToKeep
    }

    @NonCPS
    Map getAuth() {
        return this.@auth
    }

    @NonCPS
    Map getJobProperties() {
        return this.@jobProperties
    }

    @NonCPS
    Boolean getDeployOnly() {
        return this.@deployOnly
    }

    @NonCPS
    Map getJenkinsContainer() {
        return this.jenkinsContainer
    }

    @NonCPS
    Map getSlaveConfiguration() {
        return this.@slaveConfiguration
    }

    @NonCPS
    Map<String, String> getExtraEnvs() {
        return this.@extraEnvs
    }

    @NonCPS
    Map<String, String> getDependencies() {
        return this.@dependencies
    }

    @NonCPS
    List<Stage> getStages() {
        return this.@stages
    }

    @NonCPS
    Map<String, String> getKubeupConfig() {
        return this.@kubeupConfig
    }
}

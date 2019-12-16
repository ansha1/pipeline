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
import static com.nextiva.SharedJobsStaticVars.JENKINS_KUBERNETES_CLUSTER_DOMAIN
import static com.nextiva.utils.Utils.buildID


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
    @Delegate PipelineConfig pipelineConfig
    private BranchingModel branchingModel
    private Tool deployTool
    List<Environment> environmentsToDeploy
    Map slaveConfiguration
    Boolean isJobHasDependencies = false
    List<Environment> environments
    DeploymentType deploymentType
    String namespace
    String ciClusterDomain

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
        this.pipelineConfig = pipelineConfig
        this.setBranchingModel(pipelineConfig.branchingModel)
        this.@logger.trace("Branching model is ${this.@branchingModel.class.simpleName}")
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

        namespace = buildID(script.env.JOB_NAME, script.env.BUILD_ID)
        ciClusterDomain = "$namespace-$JENKINS_KUBERNETES_CLUSTER_DOMAIN"
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
        script.env['PIP_INDEX_URL'] = (deploymentType == DeploymentType.DEV) ? PIP_EXTRA_INDEX_URL_DEV :
                PIP_EXTRA_INDEX_URL_PROD
        script.env['TWINE_REPOSITORY_URL'] = (deploymentType == DeploymentType.DEV) ? TWINE_REPOSITORY_URL_DEV :
                TWINE_REPOSITORY_URL_PROD
        if (extraEnvs) {
            logger.debug("Adding extra envVars")
            extraEnvs.each { k, v ->
                logger.debug("[$k]=$v")
                script.env[k] = v
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
            logger.debug("Deploy tool is $pipelineConfig.deployTool")

            def toolConfig = ["name": pipelineConfig.deployTool]
            toolFactory.mergeWithDefaults(toolConfig)
            putSlaveContainerResource(pipelineConfig.deployTool, toolConfig)

            this.@deployTool = toolFactory.build(toolConfig)

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
    List<Stage> getStages() {
        return this.@stages
    }
}

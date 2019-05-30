package com.nextiva.config

import com.cloudbees.groovy.cps.NonCPS
import com.nextiva.environment.Environment
import com.nextiva.environment.EnvironmentFactory
import com.nextiva.stages.StageFactory
import com.nextiva.stages.stage.Stage

import static com.nextiva.SharedJobsStaticVars.getDEFAULT_CONTAINERS

class Config implements Serializable {
    // used to store all parameters passed into config
    Map configuration = [:]
    Script script

    Config(Script script, Map pipelineParams) {
        script.log.info("1 complete")
        this.script = script
        script.log.info("2 complete")
        this.configuration = pipelineParams
        script.log.info("preload complete")
        validate()
        script.log.info("preload complete")
        setDefaults()
        script.log.info("preload setDefaults() complete")
        configureEnvironment()
        script.log.info("preload configureEnvironment()complete")
        setExtraEnvVariables()
        script.log.info("preload setExtraEnvVariables() complete")
        setJobParameters()
        script.log.info("preload setJobParameters() complete")
        configureStages()
        script.log.info("preload configureStages() complete")
        configureSlave()
        script.log.info("preload configureSlave() complete")
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
    @NonCPS
    private void setDefaults() {
        //Set flags
        //Use default value, this also creates the key/value pair in the map.
        //TODO: move branching model(gitflow and trunkbased) to the class or enum
        configuration.get("branchingModel", "gitflow")
        configuration.get("jobTimeoutMinutes", "60")
        configuration.put("isUnitTestEnabled", configuration.build.any {
            it.containsKey("unitTestCommands")
        })
        configuration.put("isIntegrationTestEnabled", configuration.build.any {
            it.containsKey("integrationTestCommands")
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

    @NonCPS
    private void configureEnvironment() {
        if (configuration.get("isDeployEnabled")) {
            EnvironmentFactory environmentFactory = new EnvironmentFactory(configuration)
            List<Environment> environmentsToDeploy = environmentFactory.getAvailableEnvironmentsForBranch(configuration.get("branchName"))
            configuration.put("environmentsToDeploy", environmentsToDeploy)
        }
    }

    @NonCPS
    private void setJobParameters() {
        JobProperties jobProperties = new JobProperties(script, configuration)
        configuration.put("jobProperties", jobProperties.toMap())
    }

    @NonCPS
    private void setExtraEnvVariables() {
        configuration.extraEnvs.each { k, v -> script.env[k] = v }
    }

    @NonCPS
    private void configureSlave() {
        SlaveFactory slaveFactory = new SlaveFactory(this)
        configuration.put("slaveConfiguration", slaveFactory.getSlaveConfiguration())
    }

    @NonCPS
    private void configureStages() {
        List<Stage> stages = StageFactory.getStagesFromConfiguration(script, configuration)
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

    Map getJenkinsContainer() {
        return configuration.get("jenkinsContainer", getDEFAULT_CONTAINERS().get("jnlp"))
    }

    Map getBuildDependencies() {
        return configuration.get("dependencies")
    }

    Map getBuildConfiguration() {
        return configuration.get("build")
    }

    Map getDeployConfiguration() {
        return configuration.get("deploy")
    }

    List<Stage> getStages() {
        return configuration.get("stages")
    }
}



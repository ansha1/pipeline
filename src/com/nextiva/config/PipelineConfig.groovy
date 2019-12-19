package com.nextiva.config

import com.cloudbees.groovy.cps.NonCPS
import com.nextiva.environment.Environment
import com.nextiva.tools.deploy.DeployTool
import hudson.AbortException

import static com.nextiva.utils.Utils.*

/**
 * Used to store pipeline configuration during its initialization.
 * Later it can be used to pass all properties to {@link com.nextiva.config.Config}.
 *
 * Here are some reasons why specifying {@link com.nextiva.config.Config} as a delegate to pipeline is not a good idea:
 * <ul>
 *    <li>Config class has many fields and properties that should not be exposed to pipeline user.</li>
 *    <li>PipelineConfig can be used as a reference for all possible pipeline configuration fields and their default
 *    values.</li>
 * </ul>
 *
 * @see com.nextiva.config.Config
 */
class PipelineConfig {
    Script script

    /**
     * Application name
     */
    String appName //TODO: add default appName generation based on the repository name

    /**
     * Slack channel to send notifications
     */
    String channelToNotify

    /**
     * Override application version for deployment.<br>
     * Setting this property would force 'Deploy only' mode, where all stages up until Deploy are skipped.
     */
    String version

    /**
     * Force specified branch instead of branch name obtained from VCS
     */
    String branchName

    /**
     * Branching model to use.<br>
     * Possible values are "gitflow" and "trunkbased".<br>
     * Default value is "gitflow"
     */
    String branchingModel = "gitflow"

    /**
     * Deployment tool.<br>Possible values are "kubeup", "ansible", or "static". <br>
     * Default value is "kubeup"
     */
    String deployTool = "kubeup"

    /**
     * Whether deployment stage is enabled or not.<br>
     * Default value is true
     */
    Boolean isDeployEnabled = true

    /**
     * Time in minutes, after which the job will be terminated.<br>
     * Default value is "60"
     */
    String jobTimeoutMinutes = "60"

    /**
     * Whether unit test stage is enabled or not.<br>
     * Default value is true
     */
    Boolean isUnitTestEnabled = true

    /**
     * Whether security scan stage is enabled or not.<br>
     * Default value is true
     */
    Boolean isSecurityScanEnabled = true

    /**
     * Whether sonar scan stage is enabled or not.<br>
     * Default value is true
     */
    Boolean isSonarAnalysisEnabled = true

    /**
     * Whether QA Core Team Tests stage is enabled or not.<br>
     * Default value is true
     */
    Boolean isQACoreTeamTestEnabled = true

    /**
     * Whether Integration tests stage is enabled or not.<br>
     * Default value is false
     */
    Boolean isIntegrationTestEnabled = false

    /**
     * List of build tools definitions
     */
    List<Map> build

    /**
     * List of job triggers.
     */
    List jobTriggers = []

    /**
     * Jenkins buildDaysToKeep property.<br>
     * Default value is "30"
     */
    String buildDaysToKeep = "30"

    /**
     * Jenkins buildNumToKeep property.<br>
     * Default value is "50"
     */
    String buildNumToKeep = "50"

    /**
     * Jenkins buildArtifactDaysToKeep property.<br>
     * Default value is "10"
     */
    String buildArtifactDaysToKeep = "10"

    /**
     * Jenkins buildArtifactNumToKeep property.<br>
     * Default value is "10"
     */
    String buildArtifactNumToKeep = "10"

    /**
     * Authorization matrix definitions.
     */
    Map auth = [:]
    Map jobProperties

    /**
     * Start from deploy stage.<br>
     * Default value is false
     */
    Boolean deployOnly = false

    /**
     * Jenkins slave container customization.
     */
    Map jenkinsContainer = ["name": "jnlp"]

    /**
     * Additional slaves customization.
     */
    Map slaveConfiguration

    /**
     * Map of extra environment variables.
     */
    Map<String, String> extraEnvs = [:]

    /**
     * Map of cloud-app and cloud-platform dependencies for integration tests.
     */
    Map<String, String> dependencies = [:]

    /**
     * Additional kubeup configuration
     */
    Map<String, String> kubeupConfig = [:]

    /**
     * Additional environments definition.
     */
    private List<Environment> environments = []

    /**
     * Authorization matrix definitions.
     */
    Map branchPermissions = [:]

    @NonCPS
    List<Environment> getEnvironments() {
        return this.@environments
    }

    void setEnvironments(List envs) {
        this.@environments = envs.collect { it as Environment }
    }

    void setDeployTool(String deployTool) {
        def supportedDeployTools = [
                "kubeup",
                "ansible",
                "static"
        ]
        if (supportedDeployTools.contains(deployTool)) {
            this.deployTool = deployTool
        } else {
            throw new AbortException("Incorrect deploy tool name. Supported tools: ${supportedDeployTools.join(', ')}")
        }
    }
}

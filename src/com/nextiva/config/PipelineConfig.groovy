package com.nextiva.config

import com.cloudbees.groovy.cps.NonCPS
import com.nextiva.environment.Environment
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
    String appName
    String channelToNotify
    String version
    String branchName
    String branchingModel = "gitflow"
    String deployTool = "kubeup"
    Boolean isDeployEnabled = true
    String jobTimeoutMinutes = "60"
    Boolean isUnitTestEnabled = true
    Boolean isSecurityScanEnabled = true
    Boolean isSonarAnalysisEnabled = true
    Boolean isQACoreTeamTestEnabled = true
    Boolean isIntegrationTestEnabled = false
    Map<String, Map> build
    List jobTriggers = []
    String buildDaysToKeep = "30"
    String buildNumToKeep = "50"
    String buildArtifactDaysToKeep = "10"
    String buildArtifactNumToKeep = "10"
    Map auth = [:]
    Map jobProperties
    Boolean deployOnly = false
    Map jenkinsContainer = ["name": "jnlp"]
    Map slaveConfiguration
    Map<String, String> extraEnvs = [:]
    Boolean isJobHasDependencies = false
    Map<String, String> dependencies = [:]
    Map<String, String> kubeupConfig = [:]
    private List<Environment> environments = []
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

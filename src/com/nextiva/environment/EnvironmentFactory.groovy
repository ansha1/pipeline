package com.nextiva.environment

import com.cloudbees.groovy.cps.NonCPS
import com.nextiva.config.BranchingModel
import com.nextiva.config.GitFlow
import com.nextiva.config.TrunkBased
import com.nextiva.utils.Logger
import groovy.json.JsonOutput

class EnvironmentFactory {

    private List<Environment> environments = [
            new Environment(
                    name: "dev",
                    branchGitflow: GitFlow.develop,
                    branchTrunkbased: TrunkBased.trunk,
                    kubernetesCluster: "dev.nextiva.io",
                    kubernetesConfigSet: "aws-dev",
                    ansibleInventory: "dev"
            ),
            new Environment(
                    name: "qa",
                    branchGitflow: GitFlow.releaseOrHotfix,
                    kubernetesCluster: "qa.nextiva.io",
                    kubernetesConfigSet: "aws-qa",
                    ansibleInventory: "qa"
            ),
            new Environment(
                    name: "production",
                    branchGitflow: GitFlow.master,
                    kubernetesCluster: "prod.nextiva.io",
                    kubernetesConfigSet: "aws-prod",
                    ansibleInventory: "production"
            ),
            new Environment(
                    name: "sales-demo",
                    kubernetesCluster: "sales-demo.nextiva.io",
                    kubernetesConfigSet: "aws-sales-demo",
                    ansibleInventory: "sales-demo"
            ),
            new Environment(
                    name: "tooling",
                    kubernetesCluster: "tooling.nextiva.io",
                    kubernetesConfigSet: "aws-tooling",
            ),
            new Environment(
                    name: "sandbox",
                    kubernetesConfigSet: "test",
            ),
    ]

    private Logger logger = new Logger(this)

    EnvironmentFactory(List<Environment> environmentsFromPipeline = []) {
        logger.trace("Merging environments from pipeline with defaults")
        mergeEnvironments(environmentsFromPipeline)
        logger.trace("Merged environments:", JsonOutput.toJson(environments))
        logger.trace("${this.class.simpleName} created")
    }

    @NonCPS
    void mergeEnvironments(List<Environment> environmentsFromPipeline) {
        Map<String, Environment> environmentsMap = environments.collectEntries { [(it.name): it] }

        environmentsFromPipeline.each { environment ->
            Environment env = environmentsMap.get(environment.name, null)
            if (env != null) {
                env = (env.properties.findAll { k, v -> v } + environment.properties.findAll { k, v -> v })
                        .findAll { k, v -> k != 'class' }
            } else {
                env = environment
            }
            environmentsMap.put(environment.name, env)
        }

        this.environments = environmentsMap.collect { it.value }
    }

    @NonCPS
    List<Environment> getAvailableEnvironmentsForBranch(BranchingModel branchingModel, String branchName) {
        List<Environment> deployEnvironments = environments.findAll {
            it.getBranchPattern(branchingModel)?.matcher(branchName)?.matches()
        }
        return deployEnvironments
    }
}

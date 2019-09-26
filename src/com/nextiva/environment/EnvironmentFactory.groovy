package com.nextiva.environment

import com.cloudbees.groovy.cps.NonCPS

class EnvironmentFactory {

    private Map environment = ["dev"       : ["name"                    : "dev",
                                              "branchPattern_gitflow"   : /^(develop|dev)$/,
                                              "branchPattern_trunkbased": /^master$/,
                                              "kubernetesCluster"       : "dev.nextiva.io",
                                              "kubernetesConfigSet"     : "aws-dev",
                                              "kubernetesNamespace"     : "default",
                                              "ansibleInventory"        : "dev",
                                              "healthChecks"            : []],
                               "qa"        : ["name"                 : "qa",
                                              "branchPattern_gitflow": /^(release|hotfix)\/.+$/,
                                              "kubernetesCluster"    : "qa.nextiva.io",
                                              "kubernetesConfigSet"  : "aws-qa",
                                              "kubernetesNamespace"  : "default",
                                              "ansibleInventory"     : "qa",
                                              "healthChecks"         : []],
                               "production": ["name"                 : "prod",
                                              "branchPattern_gitflow": /^master$/,
                                              "kubernetesCluster"    : "prod.nextiva.io",
                                              "kubernetesConfigSet"  : "aws-prod",
                                              "kubernetesNamespace"  : "default",
                                              "ansibleInventory"     : "production",
                                              "healthChecks"         : []],
                               "sales-demo": ["name"               : "sales-demo",
                                              "kubernetesCluster"  : "sales-demo.nextiva.io",
                                              "kubernetesConfigSet": "aws-sales-demo",
                                              "kubernetesNamespace": "default",
                                              "ansibleInventory"   : "sales-demo",
                                              "healthChecks"       : []],
                               "tooling"   : ["name"               : "tooling",
                                              "kubernetesCluster"  : "tooling.nextiva.io",
                                              "kubernetesConfigSet": "aws-tooling",
                                              "kubernetesNamespace": "default",
                                              "healthChecks"       : []],
                               "sandbox"   : ["name"               : "sandbox",
                                              "kubernetesConfigSet": "test",
                                              "kubernetesNamespace": "default",
                                              "healthChecks"       : []],
    ]

    EnvironmentFactory(Map configuration) {
        Map deployConfiguration = configuration.subMap(["appName", "buildVersion", "kubernetesDeploymentsList", "ansiblePlaybookPath", "ansibleInventoryPath"])
        Map environmentFromJenkinsfile = configuration.get("environment", [:])
        environment.each { k, v ->
            v << deployConfiguration
            v << environmentFromJenkinsfile.get(k, [:])
        }
    }

    @NonCPS
    public List<Environment> getAvailableEnvironmentsForBranch(String branchName, String branchingModel) {
        List<Environment> environments = []
        for (env in environment) {
            if (env.value.containsKey("branchPattern_$branchingModel".toString())) {
                env.value.branchPattern = env.value."branchPattern_$branchingModel"
            }
        }
        Map envs = environment.findAll { branchName ==~ it.value.branchPattern }
        envs.each {
            environments.add(new Environment(it.value))
        }
        return environments
    }
}

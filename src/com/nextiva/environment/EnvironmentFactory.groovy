package com.nextiva.environment

import com.cloudbees.groovy.cps.NonCPS

class EnvironmentFactory {

    private Map environment = ["dev"       : ["name"               : "dev",
                                              "branchPattern"      : /^(develop|dev)$/,
                                              "kubernetesCluster"  : "dev.nextiva.io",
                                              "kubernetesConfigSet": "aws-dev",
                                              "kubernetesNamespace": "default",
                                              "ansibleInventory"   : "dev",
                                              "healthChecks"       : []],
                               "qa"        : ["name"               : "qa",
                                              "branchPattern"      : /^(release|hotfix)\/.+$/,
                                              "kubernetesCluster"  : "qa.nextiva.io",
                                              "kubernetesConfigSet": "aws-qa",
                                              "kubernetesNamespace": "default",
                                              "ansibleInventory"   : "qa",
                                              "healthChecks"       : []],
                               "production": ["name"               : "prod",
                                              "branchPattern"      : /^master$/,
                                              "kubernetesCluster"  : "prod.nextiva.io",
                                              "kubernetesConfigSet": "aws-prod",
                                              "kubernetesNamespace": "default",
                                              "ansibleInventory"   : "production",
                                              "healthChecks"       : []],
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
    public List<Environment> getAvailableEnvironmentsForBranch(String branchName) {
        List<Environment> environments = []
        Map envs = environment.findAll { branchName ==~ it.value.branchPattern }
        envs.each {
            environments.add(new Environment(it.value))
        }
        return environments
    }
}

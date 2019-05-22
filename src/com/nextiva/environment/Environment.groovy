package com.nextiva.environment

class Environment {

    String name
    String branchPattern
    String appName
    String buildVersion

    String kubernetesCluster
    String kubernetesConfigSet
    String kubernetesNamespace
    String kubernetesDeploymentsList

    String ansiblePlaybookPath
    String ansibleInventoryPath
    String ansibleInventory

    List<String> healthChecks
}


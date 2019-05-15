package com.nextiva.environment

class Dev {
    String name //= "Dev"
    String kubernetesCluster //= "dev.nextiva.io"
    String kubernetesConfigset
    String kubernetesNamespace
    String kubernetesServiceName
    String kubernetesBuildversion
    String kubernetesDeploymentsList

    String ansiblePlaybookPath  // = 'ansible/role-based_playbooks/java-app.yml'
    String ansibleInventoryPath  // = 'ansible/role-based_playbooks/inventory/java-app'
    String ansibleInventory   // = dev
    String ansibleArgs   //= 'args'

}
//
//
//
//Map props = ["deploy":["enabled":true,
//kubernetes]]

//JsonElement jsonElement = gson.toJsonTree(map);
//MyPojo pojo = gson.fromJson(jsonElement, MyPojo.class);
//
//deploy:
//  enabled: true
//  order: "parallel|in series"
//  ansible:
//    enabled: true
//    ansibleRepo: "repo"
//    ansibleRepoBranch: "master"
//    ansibleInventoryPath: 'ansible/role-based_playbooks/inventory/java-app/dev'
//    ansiblePlaybookPath: 'ansible/role-based_playbooks/java-app.yml'
//    ansibleArgs: 'args'
//    healthchecks: []
//  kubernetes:
//    enabled: true
//    image: kubeprovisioningimage
//    appName: "interaction"
//    buildversion: "1.3.0"
//    cluster:"dev.nextiva.io"
//    namespace: "default"
//    configset: "aws-dev"
//    kubernetesDeploymentsList: ["interaction"]
//    healthchecks: []
//
//
//
//rc
//dev
//deploy:
//    type:Ansible
//    ansibleImage: "ansibleimage"
//    ansibleRepo: "repo"
//    ansibleRepoBranch: "master"
//    ansibleInventoryPath: 'ansible/role-based_playbooks/inventory/java-app/dev'
//    ansiblePlaybookPath: 'ansible/role-based_playbooks/java-app.yml'
//    healthchecks
//    ansibleArgs: 'args'
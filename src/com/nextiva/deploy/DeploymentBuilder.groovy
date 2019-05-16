package com.nextiva.deploy

import com.nextiva.deploy.tool.*

import hudson.AbortException

class DeploymentBuilder {

    Map deployment = ["name"         : "Ansible",
                      "image"        : "ansibleimage",
                      "repository"   : "repo",
                      "branch"       : "master",
                      "inventoryPath": 'ansible/role-based_playbooks/inventory/java-app/dev',
                      "playbookPath" : 'ansible/role-based_playbooks/java-app.yml',
                      "ansibleArgs"  : 'args']


    static DeploymentTool build(Map deployment){
        switch (deployment.get("type")){
            case "Ansible":
                return new Ansible(deployment)
                break
            case "Kubernetes":
                return new Kubernetes(deployment)
                break
            default:
                throw new AbortException("Can't create deployment class from map $deployment")
        }

    }
}

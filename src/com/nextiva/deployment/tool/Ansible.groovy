package com.nextiva.deployment.tool

import groovy.transform.ToString

//@ToString
class Ansible implements DeploymentTool{
    String type
    String image //ansible image
    String repository    //dev, qa, production, sales-demo
    String branch
    String playbookPath
    String inventoryPath
    String args
    List healthchecks

    Boolean deploy(){
        println("this is ansible deployment" + toString())
        return true
    }

    @Override
    public String toString() {
        return "Ansible{" +
                "type='" + type + '\'' +
                ", image='" + image + '\'' +
                ", repository='" + repository + '\'' +
                ", branch='" + branch + '\'' +
                ", playbookPath='" + playbookPath + '\'' +
                ", inventoryPath='" + inventoryPath + '\'' +
                ", args='" + args + '\'' +
                ", healthchecks=" + healthchecks +
                '}';
    }
}

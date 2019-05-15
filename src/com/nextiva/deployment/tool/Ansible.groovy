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

}

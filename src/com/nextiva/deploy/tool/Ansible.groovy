package com.nextiva.deploy.tool

class Ansible implements DeploymentTool {
    String image //ansible image
    String repository    //dev, qa, production, sales-demo
    String branch
    String playbookPath
    String inventoryPath
    String args
    List healthchecks

    Boolean deploy() {
        println("this is ansible deployment" + toString())
        return true
    }

    Ansible(Map type, String image, String repository, String branch, String playbookPath, String inventoryPath, String args, List healthchecks) {
        this.type = type
        this.image = image
        this.repository = repository
        this.branch = branch
        this.playbookPath = playbookPath
        this.inventoryPath = inventoryPath
        this.args = args
        this.healthchecks = healthchecks
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

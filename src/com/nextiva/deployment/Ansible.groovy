package com.nextiva.deployment

class Ansible implements DeploymentTool{
    private final String image //ansible image
    private final String name    //dev, qa, production, sales-demo
    private final String ansibleRepository
    private final String branch
    private final String playbookPath
    private final String inventoryPath
    private final List healthCheckEndpoints

    Ansible(String image, String name, String ansibleRepository, String branch, String playbookPath, String inventoryPath, List healthCheckEndpoints) {
        this.image = image
        this.name = name
        this.ansibleRepository = ansibleRepository
        this.branch = branch
        this.playbookPath = playbookPath
        this.inventoryPath = inventoryPath
        this.healthCheckEndpoints = healthCheckEndpoints
    }

    Boolean deploy(Map playbookContext){
        return true
    }

    String getImage() {
        return image
    }

    String getName() {
        return name
    }

    String getAnsibleRepository() {
        return ansibleRepository
    }

    String getBranch() {
        return branch
    }

    String getPlaybookPath() {
        return playbookPath
    }

    String getInventoryPath() {
        return inventoryPath
    }

    List getHealthCheckEndpoints() {
        return healthCheckEndpoints
    }


    @Override
    public String toString() {
        return "Ansible{" +
                "image='" + image + '\'' +
                ", name='" + name + '\'' +
                ", ansibleRepository='" + ansibleRepository + '\'' +
                ", branch='" + branch + '\'' +
                ", playbookPath='" + playbookPath + '\'' +
                ", inventoryPath='" + inventoryPath + '\'' +
                ", healthCheckEndpoints=" + healthCheckEndpoints +
                '}';
    }
}

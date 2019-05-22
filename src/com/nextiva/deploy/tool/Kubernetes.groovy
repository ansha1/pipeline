package com.nextiva.deploy.tool

import com.nextiva.environment.Environment

class Kubernetes extends DeployTool {
    Kubernetes(Script script, List<Environment> environments, Map configuration) {
        super(script, environments, configuration)
    }
    @Override
    Boolean deploy() {
        println("this is kubernetes deployment" + toString())
        return true
    }
}

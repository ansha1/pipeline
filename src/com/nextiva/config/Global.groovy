package com.nextiva.config

import com.nextiva.environment.Environment
import com.nextiva.tools.deploy.DeployTool

// For unknown reason @Singleton annotation did not work well when running on Jenkins
class Global implements Serializable {
    String appName
    String globalVersion
    String branchName
    String branchingModel
    DeployTool deployTool
    Boolean isDeployEnabled = true
    List<Environment> environmentsToDeploy

    private static Global single_instance = null

    private Global() {}

    static Global getInstance() {
        if (single_instance == null) {
            single_instance = new Global()
        }
        return single_instance
    }
}

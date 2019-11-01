package com.nextiva.config

import com.nextiva.environment.Environment
import com.nextiva.tools.deploy.DeployTool
import com.nextiva.utils.Logger
import hudson.AbortException

// For unknown reason @Singleton annotation did not work well when running on Jenkins
class Global implements Serializable {

//    private static Global single_instance = null

    private Global() {}

//    static Global getInstance() {
//        if (single_instance == null) {
//            single_instance = new Global()
//        }
//        return single_instance
//    }
}


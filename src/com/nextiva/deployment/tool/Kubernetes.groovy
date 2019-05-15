package com.nextiva.deployment.tool

import groovy.transform.ToString

//@ToString
class Kubernetes implements DeploymentTool {
    String image    //: kubeprovisioningimage
    String appName  //: "interaction"
    String buildversion //: "1.3.0"
    String cluster  //:"dev.nextiva.io"
    String namespace //: "default"
    String configset //: "aws-dev"
    String kubernetesDeploymentsList //: ["interaction"]
    String healthchecks //: []

    @Override
    Boolean deploy() {
        println("this is kubernetes deployment" + toString())
        return true
    }
}

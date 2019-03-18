package com.nextiva.stages

import com.nextiva.stages.BasicStage

class Checkout extends BasicStage {
    protected Checkout(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.checkout([
                $class: 'GitSCM',
                branches: scm.branches,
                doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
                extensions: scm.extensions,
                userRemoteConfigs: scm.userRemoteConfigs
        ])
    }
}

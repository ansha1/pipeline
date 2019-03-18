package com.nextiva.stages

import com.nextiva.stages.BasicSage

class Checkout extends BasicSage {
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

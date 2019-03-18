package com.nextiva.stages

import com.nextiva.stages.BasicStage

class Checkout extends BasicStage {
    protected Checkout(script, configuration) {
        super(script, configuration)
    }

    def execute(){

        script.stage("Build version verification") {
            script.checkout([
                    $class                           : 'GitSCM',
                    branches                         : script.scm.branches,
                    doGenerateSubmoduleConfigurations: script.scm.doGenerateSubmoduleConfigurations,
                    extensions                       : script.scm.extensions,
                    userRemoteConfigs                : script.scm.userRemoteConfigs
            ])
        }
    }
}

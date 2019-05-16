package com.nextiva.stages.stage

class Checkout extends Stage {
    Checkout(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def execute(){
        script.stage(this.getClass().getSimpleName()) {
            script.checkout([
                    $class                           : 'GitSCM',
                    branches                         : script.scm.branches,
                    doGenerateSubmoduleConfigurations: script.scm.doGenerateSubmoduleConfigurations,
                    extensions                       : script.scm.extensions,
                    userRemoteConfigs                : script.scm.userRemoteConfigs
            ])

             script.sh(
                    script: "ls -la",
                    returnStdout: true
            ).trim()
        }
    }
}

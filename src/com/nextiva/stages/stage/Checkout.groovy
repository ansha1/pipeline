package com.nextiva.stages.stage

class Checkout extends Stage {
    Checkout(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def execute() {
        log.debug("this is checkout debug message")
        log.info("this is checkout info message")
        log.trace("this is checkout trace message")
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

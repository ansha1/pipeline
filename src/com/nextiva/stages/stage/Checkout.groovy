package com.nextiva.stages.stage

import com.nextiva.utils.Logger

class Checkout extends Stage {
    Logger log = new Logger(this)
    Checkout(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        try {
            sssss
            script.checkout([
                    $class                           : 'GitSCM',
                    branches                         : script.scm.branches,
                    doGenerateSubmoduleConfigurations: script.scm.doGenerateSubmoduleConfigurations,
                    extensions                       : script.scm.extensions,
                    userRemoteConfigs                : script.scm.userRemoteConfigs
            ])
        } catch (e) {
            log.error("Error when executing ${stageName()}:", e)
            throw e
        }
    }
}

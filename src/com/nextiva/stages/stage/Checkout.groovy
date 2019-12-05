package com.nextiva.stages.stage

import static com.nextiva.config.Config.instance as config

class Checkout extends Stage {
    Checkout() {
        super()
    }

    @Override
    def stageBody() {
        try {
            config.script.checkout([
                    $class                           : 'GitSCM',
                    branches                         : config.script.scm.branches,
                    doGenerateSubmoduleConfigurations: config.script.scm.doGenerateSubmoduleConfigurations,
                    extensions                       : config.script.scm.extensions,
                    userRemoteConfigs                : config.script.scm.userRemoteConfigs
            ])
        } catch (e) {
            logger.error("Error when executing ${stageName}: ", e)
            throw e
        }
    }
}

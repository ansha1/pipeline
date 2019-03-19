package com.nextiva.stages

import com.nextiva.stages.BasicStage

class BuildArtifact extends BasicStage {
    protected BuildArtifact(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
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

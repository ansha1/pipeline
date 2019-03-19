package stages

import com.nextiva.stages.BasicStage

class SendNotifications extends BasicStage {
    protected SendNotifications(script, configuration) {
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

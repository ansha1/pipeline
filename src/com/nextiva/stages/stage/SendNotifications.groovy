package com.nextiva.stages.stage

import static com.nextiva.config.Config.instance as config

class SendNotifications extends Stage {
    SendNotifications() {
        super()
    }

    @Override
    def stageBody() {
        config.script.container("jnlp") {
            //TODO: refactor for the native class usage
            config.script.slack.sendBuildStatus(config.channelToNotify)
            if (config.branchName ==~ /^(PR.+)$/) {
                config.script.slack.prOwnerPrivateMessage(config.script.env.CHANGE_URL)
            }
//            if (branchName ==~ /^(release\/.+)$/) {
//                String appName = configuration.get("appName")
//                String buildVersion = configuration.get("buildVersion")
//                script.slack.deployFinish(appName, buildVersion, "qa", SLACK_STATUS_REPORT_CHANNEL_RC)
//            }
        }
    }
}

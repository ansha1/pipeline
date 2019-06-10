package com.nextiva.stages.stage

import static com.nextiva.SharedJobsStaticVars.SLACK_STATUS_REPORT_CHANNEL_RC

class SendNotifications extends Stage {
    SendNotifications(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        script.container("jnlp") {
            //TODO: refactor for the native class usage
            script.slack.sendBuildStatus(configuration.get("channelToNotify"))
            String branchName = configuration.get("branchName")
            if (branchName ==~ /^(PR.+)$/) {
                script.slack.prOwnerPrivateMessage(script.env.CHANGE_URL)
            }
            if (branchName ==~ /^(release\/.+)$/) {
                String appName = configuration.get("appName")
                String buildVersion = configuration.get("buildVersion")
                script.slack.deployFinish(appName, buildVersion, "qa", SLACK_STATUS_REPORT_CHANNEL_RC)
            }
        }
    }
}

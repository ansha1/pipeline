package com.nextiva.stages.stage

import com.nextiva.config.BranchingModelRegexps

import static com.nextiva.config.Config.instance as config

class SendNotifications extends Stage {
    SendNotifications() {
        super()
    }

    @Override
    def stageBody() {
        def script = config.script
        script.container("jnlp") {
            //TODO: refactor for the native class usage
            script.slack.sendBuildStatus(config.channelToNotify)

            if (config.branchName ==~ /^(PR.+)$/) {
                script.slack.prOwnerPrivateMessage(script.env.CHANGE_URL)
                script.jiraSendBuildInfo site: 'nextiva.atlassian.net', branch: script.env.CHANGE_BRANCH
            } else if (config.branchName ==~ BranchingModelRegexps.notMainline) {
                script.jiraSendBuildInfo site: 'nextiva.atlassian.net'
            }
//            if (branchName ==~ /^(release\/.+)$/) {
//                String appName = configuration.get("appName")
//                String buildVersion = configuration.get("buildVersion")
//                script.slack.deployFinish(appName, buildVersion, "qa", SLACK_STATUS_REPORT_CHANNEL_RC)
//            }
        }
    }
}

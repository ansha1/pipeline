import com.nextiva.slack.MessagesFactory
import static com.nextiva.SharedJobsStaticVars.*


/**
 * Send approval request to Slack user or channel.
 *
 * @param title - Title text
 * @param text - Main text
 * @param slackReceiver - Slack user or channel identifier, should be of the format `<#C12345>`, `<@U12345>`, `<@U12345|user>`, `@user`, `#channel/user` or `#channel`
 * @param yesText - Approval button text
 * @param noText - Decline button text
 * @param authorizedApprovers - List of usernames
 */
def call(String title = 'Should we proceed?', String text = 'Jenkins is waiting for your approval',
         String slackReceiver, String yesText = 'Approve', String noText = 'Decline',
         List authorizedApprovers = []) {

    if(JENKINS_BOT_ENABLE) {
        // Send Slack interactive message through Jenkins Bot
        String titleLink = "${BUILD_URL}input/"

        return bot.getJenkinsApprove(slackReceiver, yesText, noText, title, titleLink, text, titleLink,
                authorizedApprovers)
    }
    else {
        // Send a non-interactive message directly to Slack
        def slackMessage = new MessagesFactory(this).buildApproveMessage(title, text)
        slack.sendPrivatMessage(slackReceiver, slackMessage)

        return input(id: 'Proceed', message: text, ok: yesText, submitter: authorizedApprovers.join(","),
                submitterParameter: 'approver')
    }
}

@Deprecated
def sendToPrivate(String title, String UserSlackId, List authorizedApprovers, Integer minutes = 5) {
    log.deprecated('Use approve() method.')
    call(title, UserSlackId, authorizedApprovers, minutes)
}

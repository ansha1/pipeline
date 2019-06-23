import com.nextiva.slack.MessagesFactory
import static com.nextiva.SharedJobsStaticVars.*


def call(String title = 'Should we proceed?', String text = 'Jenkins is waiting for your approval',
         String UserSlackId, String yesText = 'Approve', String noText = 'Decline',
         List authorizedApprovers = [], Integer minutes = 15) {

    timeout(minutes) {

        if(JENKINS_BOT_ENABLE) {
            String titleLink = "${BUILD_URL}input/"

            return bot.getJenkinsApprove(UserSlackId, yesText, noText, title, titleLink, text, titleLink,
                    authorizedApprovers)
        }
        else {
            def slackMessage = new MessagesFactory(this).buildApproveMessage(title, text)
            slack.sendPrivatMessage(UserSlackId, slackMessage)

            return input(id: 'Proceed', message: text, ok: yesText, submitter: authorizedApprovers.join(","),
                    submitterParameter: 'approver')
        }
    }
}

@Deprecated
def sendToPrivate(String title, String UserSlackId, List authorizedApprovers, Integer minutes = 5) {
    log.deprecated('Use approve() method.')
    call(title, UserSlackId, authorizedApprovers, minutes)
}

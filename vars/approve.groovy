import com.nextiva.slack.MessagesFactory

def call(String title = 'Should we proceed?', String slackChannel, String authorizedApprovers, Integer minutes = 5) {
    timeout(minutes) {
        def slackMessage = new MessagesFactory(this).buildApproveMessage(message)
        slack.sendMessage(slackChannel, slackMessage)
        return input(id: 'Proceed', message: title, ok: 'Approve', submitter: authorizedApprovers, submitterParameter: 'approver')
    }
}

def sendToPrivate(String message = 'Should we proceed?' , String UserSlackId, List authorizedApprovers, Integer minutes = 5) {
    timeout(minutes) {
        def slackMessage = new MessagesFactory(this).buildApproveMessage(message)
        slack.sendMessage(UserSlackId, slackMessage)
        return input(id: 'Proceed', message: message, ok: 'Approve', submitter: authorizedApprovers.join(","),
                submitterParameter: 'approver')
    }
}
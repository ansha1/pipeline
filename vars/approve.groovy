import com.nextiva.slack.MessagesFactory


def call(String message = 'Should we proceed?', String UserSlackId, List authorizedApprovers = [], Integer minutes = 5) {
    timeout(minutes) {
        print('111111111')
        SlackMessage slackMessage = new MessagesFactory(this).buildApproveMessage(message)
        print('2222222222')
        slack.sendMessage(UserSlackId, slackMessage)
        print('3333333333')
        return input(id: 'Proceed', message: message, ok: 'Approve', submitter: authorizedApprovers.join(","),
                submitterParameter: 'approver')
    }
}

@Deprecated
def sendToPrivate(String message = 'Should we proceed?', String UserSlackId, List authorizedApprovers, Integer minutes = 5) {
    log.deprecated('Use approve() method.')
    call(message, UserSlackId, authorizedApprovers, minutes)
}

def call(String message = 'Should we proceed', String slackChannel, String authorizedApprovers, Integer minutes = 5) {
    timeout(minutes) {

        def buildMessage = buildApproveMessageBody(message)
        slack(slackChannel, buildMessage)
        def inputResponse = input(id: 'Proceed', message: message, ok: 'Approve', submitter: authorizedApprovers, submitterParameter: 'approver')
    }
}

def sendToPrivate(String message = 'Should we proceed' , String UserSlackId, Integer minutes = 5) {
    timeout(minutes) {
        def buildMessage = buildApproveMessageBody(message)
        slack.privateMessage(UserSlackId, buildMessage)
        def inputResponse = input(id: 'Proceed', message: message, ok: 'Approve')
    }
}

def buildApproveMessageBody(String message) {
    def uploadSpec = """[
        {
            "title": "${message}",
            "text": "STAGE ${env.STAGE_NAME} in ${JOB_NAME} is waiting for your approval",
            "color": "#022ef2",
            "attachment_type": "default",
            "actions": [
                {
                    "text": "Approve",
                    "type": "button",
                    "url": "${env.BUILD_URL}input"
                }
            ]
        }
        ]"""

    return uploadSpec
}
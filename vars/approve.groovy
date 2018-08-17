import java.net.URLDecoder


def call(String title = 'Should we proceed?', String slackChannel, String authorizedApprovers, Integer minutes = 5) {
    timeout(minutes) {

        def buildMessage = buildApproveMessageBody(title)
        slack(slackChannel, buildMessage)
        return input(id: 'Proceed', message: title, ok: 'Approve', submitter: authorizedApprovers, submitterParameter: 'approver')
    }
}

def sendToPrivate(String message = 'Should we proceed?' , String UserSlackId, List authorizedApprovers, Integer minutes = 5) {
    timeout(minutes) {
        def buildMessage = buildApproveMessageBody(message)
        slack.privateMessage(UserSlackId, buildMessage)
        return input(id: 'Proceed', message: message, ok: 'Approve', submitter: authorizedApprovers.join(","),
                submitterParameter: 'approver')
    }
}

def buildApproveMessageBody(String title) {
    String jobName = URLDecoder.decode(env.JOB_NAME.toString(), 'UTF-8')

    def uploadSpec = """[
        {
            "title": "${title}",
            "text": "Stage \"${env.STAGE_NAME}\" in ${jobName} is waiting for your approval",
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
def call(String message='Should we proceed', String slackChannel, String authorizedApprovers, Integer minutes=5) {
    timeout(minutes) {
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

        slackSend(channel: slackChannel, attachments: uploadSpec, tokenCredentialId: "slackToken")
        def inputResponse = input(id: 'Proceed', message: message, ok: 'Approve', submitter: authorizedApprovers, submitterParameter: 'approver')
    }
}
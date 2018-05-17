def call(String notifyChannel) {
    buildStatus = currentBuild.currentResult
    notifyColor = ['SUCCESS': '#00FF00', 'FAILURE': '#FF0000', 'UNSTABLE': '#FF0000']
    commitinforaw = sh returnStdout: true, script: "git show --pretty=format:'The author was %an, %ar. Commit message: %s' | sed -n 1p"
    commitinfo = commitinforaw.trim()
    def subject = "Build status: ${buildStatus} Job: ${env.JOB_NAME.replaceAll("%2F","_")} #${env.BUILD_ID}"
    def uploadSpec = """[
        {
            "title": "${subject}",
            "text": "${commitinfo}",
            "color": "${notifyColor.get(buildStatus)}",
            "attachment_type": "default",
            "actions": [
                {
                    "text": "Console output",
                    "type": "button",
                    "url": "${env.BUILD_URL}console"
                },
                {
                    "text": "Test results",
                    "type": "button",
                    "url": "${env.BUILD_URL}testReport"
                }
            ]
        }
    ]"""
    slackSend(channel: notifyChannel, attachments: uploadSpec, tokenCredentialId: "slackToken")
}

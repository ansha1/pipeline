#!groovy
import java.net.URLEncoder
import static com.nextiva.SharedJobsStaticVars.*

def call(String notifyChannel) {
    def uploadSpec = buildStatusMessageBody()
    slackSend(channel: notifyChannel, attachments: uploadSpec, tokenCredentialId: "slackToken")
}

def commitersOnly() {
    try {
        def uploadSpec = buildStatusMessageBody()
        def commitAuthors = getCommitAuthors()
        commitAuthors.each {
            def slackUserId = getSlackUserIdByEmail(it)
            privateMessage(slackUserId, uploadSpec)
        }
    } catch (e) {
        log.warn("Failed send Slack notication to the commit authors: " + e.toString())
    }
}

def prOwnerPrivateMessage() {
    def slackUserId = bitbucket.prOwner()
    privateMessage(slackUserId, uploadSpec)
}

def privateMessage(String slackUserId, String message) {
    log.debug("Message: " + message)
    def attachments = java.net.URLEncoder.encode(message, "UTF-8")
    httpRequest contentType: 'APPLICATION_JSON', quiet: env.DEBUG ? false : true,
            consoleLogResponseBody: env.DEBUG ? false : true, httpMode: 'POST',
            url: "https://nextivalab.slack.com/api/chat.postMessage?token=${SLACK_BOT_TOKEN}&channel=${slackUserId}&as_user=true&attachments=${attachments}"

}

def buildStatusMessageBody() {
    buildStatus = currentBuild.currentResult
    notifyColor = ['SUCCESS': '#00FF00', 'FAILURE': '#FF0000', 'UNSTABLE': '#FF0000']
    commitinforaw = sh returnStdout: true, script: "git show --pretty=format:'The author was %an, %ar. Commit message: %s' | sed -n 1p"
    commitinfo = commitinforaw.trim()
    def subject = "Build status: ${buildStatus} Job: ${env.JOB_NAME.replaceAll("%2F", "_")} #${env.BUILD_ID}"
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
    return uploadSpec
}

def getSlackUserIdByEmail(String userMail) {
    def response = httpRequest quiet: env.DEBUG ? false : true, consoleLogResponseBody: env.DEBUG ? false : true, url: "https://nextivalab.slack.com/api/users.lookupByEmail?token=${SLACK_BOT_TOKEN}&email=${userMail}"
    def responseJson = readJSON text: response.content
    return responseJson.user.id
}

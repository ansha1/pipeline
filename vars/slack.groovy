#!groovy
import java.net.URLEncoder
import static com.nextiva.SharedJobsStaticVars.*


def call(String notifyChannel, def uploadSpec) {
    log.debug(uploadSpec)
    slackSend(channel: notifyChannel, attachments: uploadSpec, tokenCredentialId: "slackToken")
}

def sendBuildStatus(String notifyChannel){
    def uploadSpec = buildStatusMessageBody()
    call(notifyChannel, uploadSpec)
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

def prOwnerPrivateMessage(String url) {
    def prOwner = bitbucket.prOwnerEmail(url)
    def uploadSpec = buildStatusMessageBody()
    privateMessage(prOwner, uploadSpec)
}

def privateMessage(String slackUserId, String message) {
    log.debug("Message: " + message)
    def attachments = java.net.URLEncoder.encode(message, "UTF-8")
    httpRequest contentType: 'APPLICATION_JSON', quiet: !log.isDebug(),
        consoleLogResponseBody: log.isDebug(), httpMode: 'POST',
        url: "https://nextivalab.slack.com/api/chat.postMessage?token=${SLACK_BOT_TOKEN}&channel=${slackUserId}&as_user=true&attachments=${attachments}"
}

def buildStatusMessageBody() {
    buildStatus = currentBuild.currentResult
    commitinforaw = sh returnStdout: true, script: "git show --pretty=format:'The author was %an, %ar. Commit message: %s' | sed -n 1p"
    commitinfo = commitinforaw.trim()
    def subject = "Build status: ${buildStatus} Job: ${env.JOB_NAME.replaceAll("%2F", "_")} #${env.BUILD_ID}"
    def uploadSpec = """[
        {
            "title": "${subject}",
            "text": "${commitinfo}",
            "color": "${SLACK_NOTIFY_COLORS.get(buildStatus)}",
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
    def response = httpRequest quiet: !log.isDebug(), consoleLogResponseBody: log.isDebug(), url: "https://nextivalab.slack.com/api/users.lookupByEmail?token=${SLACK_BOT_TOKEN}&email=${userMail}"
    def responseJson = readJSON text: response.content
    return responseJson.user.id
}

/*def notifyReleaseHotfix(String, notifyChannel, String ver, String actionType  = 'Release', String actionState = 'started'){
    buildStatus = currentBuild.currentResult
    user = common.getCurrentUser()

    def actionResult = buildStatus == 'SUCCESS' ? 'successfully' : 'unsuccessfully'
    def subject = "${actionType} ${ver} ${actionState} ${actionResult}!"  // example: Release 1.2.0 started successfuly

    def message = "Author: ${user}"
    def uploadSpec = """[
        {
            "title": "${subject}",
            "text": "${message}",
            "color": "${SLACK_NOTIFY_COLORS.get(buildStatus)}",
            "attachment_type": "default"
        }
    ]"""

    call(notifyChannel, uploadSpec)
}*/

def sendBuildStatusPrivatMessage(String userEmail){
    def slackUserId = getSlackUserIdByEmail(userEmail)
    def uploadSpec = buildStatusMessageBody()
    try {
        privateMessage(slackUserId, uploadSpec)
    } catch (e) {
        log.warn("Failed send Slack notication to the authors: " + e.toString())
    }
}

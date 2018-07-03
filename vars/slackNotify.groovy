#!groovy
import static com.nextiva.SharedJobsStaticVars.*

def call(String notifyChannel) {
    def uploadSpec = buildStatusMessageBody()
    slackSend(channel: notifyChannel, attachments: uploadSpec, tokenCredentialId: "slackToken")
}

def commitersOnly() {
    def uploadSpec = buildStatusMessageBody()
    def commitAuthor = sh returnStdout: true, script: "git show --pretty=format:'%ae' | sed -n 1p"
    def slackUserId = getSlackUserIdByEmail(commitAuthor.trim())
    privateMessage(slackUserId, uploadSpec)
}

def privateMessage(String slackUserId, String message) {
    log.debug { "Message: " + message }
    //curl -X POST -d "text=${uploadSpec}" "https://nextivalab.slack.com/api/chat.postMessage?token=${SLACK_BOT_TOKEN}&channel=${slackUserId}&as_user=true"
    //curl -H 'Accept: application/json' -X PUT '${uploadSpec}' "https://nextivalab.slack.com/api/chat.postMessage?token=${SLACK_BOT_TOKEN}&channel=${slackUserId}&as_user=true"
    httpRequest contentType: 'APPLICATION_JSON', consoleLogResponseBody: true, httpMode: 'PUT', requestBody: 'text=' + message, url: "https://nextivalab.slack.com/api/chat.postMessage?token=${SLACK_BOT_TOKEN}&channel=${slackUserId}&as_user=true"
    //slackSend(channel: slackUserId, attachments: message, tokenCredentialId: "slackToken", botUser: true)

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
    def response = httpRequest url: "https://nextivalab.slack.com/api/users.lookupByEmail?token=${SLACK_BOT_TOKEN}&email=${userMail}"
    def responseJson = readJSON text: response.content
    return responseJson.user.id
}

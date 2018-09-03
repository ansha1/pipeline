#!groovy
import java.net.URLEncoder
import static com.nextiva.SharedJobsStaticVars.*
import java.net.URLDecoder


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
    call(slackUserId, message)
}

def buildStatusMessageBody() {
    def buildStatus = currentBuild.currentResult
    def commitInfoRaw = sh returnStdout: true, script: "git show --pretty=format:'The author was %an, %ar. Commit message: %s' | sed -n 1p"
    def commitInfo = commitInfoRaw.trim()
    String jobName = URLDecoder.decode(env.JOB_NAME, 'UTF-8')
    def subject = "Build status: ${buildStatus} Job: ${jobName} #${env.BUILD_ID}"
    def uploadSpec = """[
        {
            "title": "${subject}",
            "text": "${commitInfo}",
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

    if( responseJson.ok ) {
        return responseJson.user.id
    } else {
        currentBuild.rawBuild.result = Result.ABORTED
        throw new hudson.AbortException("\n\nUser mail in Slack ${userMail} doesn't match the one that is defined in Jenkins (LDAP) !!!\n\n")
    }
}

def sendBuildStatusPrivatMessage(String slackUserId){
    def uploadSpec = buildStatusMessageBody()
    try {
        privateMessage(slackUserId, uploadSpec)
    } catch (e) {
        log.warn("Failed send Slack notication to the authors: " + e.toString())
    }
}

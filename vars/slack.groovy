#!groovy
import java.net.URLEncoder
import static com.nextiva.SharedJobsStaticVars.*
import java.net.URLDecoder
import java.util.Random


def call(String notifyChannel, def uploadSpec) {
    log.debug(uploadSpec)
    slackSend(channel: notifyChannel, attachments: uploadSpec, tokenCredentialId: "slackToken")
}

def sendBuildStatus(String notifyChannel) {
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
        log.warn("Failed send Slack notification to the commit authors: " + e.toString())
    }
}

def prOwnerPrivateMessage(String url) {
    def prOwner = bitbucket.prOwnerEmail(url)
    log.info("prOwner - ${prOwner}")
    def uploadSpec = buildStatusMessageBody()
    log.info("uploadSpec - ${uploadSpec}")
    privateMessage(prOwner, uploadSpec)
}

def privateMessage(String slackUserId, String message) {
    log.info("Message: " + message)
    def attachments = java.net.URLEncoder.encode(message, "UTF-8")
    log.info("attachments: ${attachments}")
    httpRequest contentType: 'APPLICATION_JSON', quiet: !log.isDebug(),
            consoleLogResponseBody: log.isDebug(), httpMode: 'POST',
            url: "https://nextivalab.slack.com/api/chat.postMessage?token=${SLACK_BOT_TOKEN}&channel=${slackUserId}&as_user=true&attachments=${attachments}"
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

    if (responseJson.ok) {
        return responseJson.user.id
    } else {
        log.warn("\n\nUser mail in Slack ${userMail} doesn't match the one that is defined in Jenkins (LDAP) !!!\n\n return the default channel #testchannel <C9FRGVBPB>")
        def defaultId = 'C9FRGVBPB'
        return defaultId
    }
}

def sendBuildStatusPrivateMessage(String slackUserId) {
    def uploadSpec = buildStatusMessageBody()
    try {
        privateMessage(slackUserId, uploadSpec)
    } catch (e) {
        log.warn("Failed send Slack notification to the authors: " + e.toString())
    }
}

def deployStart(String appName, String version, String environment, String notifyChannel) {
    def triggeredBy = getSlackUserIdByEmail(common.getCurrentUserEmail())
    def message = "`${appName}:${version}` ${environment.toUpperCase()} deploy started by <@${triggeredBy}>"

    slackSend(channel: notifyChannel, color: SLACK_NOTIFY_COLORS.get(currentBuild.currentResult), message: message, tokenCredentialId: "slackToken")
}

def deployFinish(String appName, String version, String environment, String notifyChannel) {

    def wishList = libraryResource('wishes.txt').readLines()
    def randomWish = wishList[pickRandomNumber(wishList.size())]
    def buildStatus = currentBuild.currentResult == "SUCCESS" ? "deployed :tada: ${randomWish}" : "deploy failed! :disappointed:"

    def message = "`${appName}:${version}` ${environment.toUpperCase()} ${buildStatus}"

    slackSend(channel: notifyChannel, color: SLACK_NOTIFY_COLORS.get(currentBuild.currentResult), message: message, tokenCredentialId: "slackToken")
}

Integer pickRandomNumber(Integer max) {
    Random rand = new Random()
    return rand.nextInt(max)
}
#!groovy
import com.nextiva.slack.dto.SlackMessage
import com.nextiva.slack.MessagesFactory
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import static com.nextiva.SharedJobsStaticVars.*


def call(String notifyChannel, def uploadSpec) {
    slackSend(channel: notifyChannel, attachments: uploadSpec, tokenCredentialId: 'slackToken')
}

@Deprecated
def privateMessage(String slackUserId, String uploadSpec) {
    log.deprecated('Use slack.sendMessage() method.')
    log.debug("uploadSpec: " + uploadSpec)
    def attachments = java.net.URLEncoder.encode(uploadSpec, "UTF-8")
    httpRequest contentType: 'APPLICATION_JSON', quiet: !log.isDebug(),
            consoleLogResponseBody: log.isDebug(), httpMode: 'POST',
            url: "${SLACK_URL}/api/chat.postMessage?token=${SLACK_BOT_TOKEN}&channel=${slackUserId}&as_user=true&attachments=${attachments}"
}

@SuppressWarnings("GroovyAssignabilityCheck")
def sendMessage(String notifyChannel, SlackMessage message) {
    message.setChannel(notifyChannel)
    log.debug(toJson(message))
    def response = httpRequest contentType: 'APPLICATION_JSON_UTF8',
                               quiet: !log.isDebug(),
                               consoleLogResponseBody: log.isDebug(),
                               httpMode: 'POST',
                               url: "${SLACK_URL}/api/chat.postMessage",
                               customHeaders: [[name: 'Authorization', value: "Bearer ${SLACK_BOT_TOKEN}"]],
                               requestBody: toJson(message)
                               validResponseCodes: '100:599'

    def jsonResponse = new JsonSlurper().parseText(response.content)
    if(jsonResponse.error) {
        log.warning("Slack send error message: ${jsonResponse.error}")

        if(jsonResponse.error == 'channel_not_found' || jsonResponse.error == 'not_in_channel') {
            log.magnetaBold("""
            Jenkins can't send the notification into the Slack channel: ${notifyChannel}
            Slack channel does not exist or the Jenkins user is not a member of private channel.
            """)
        }
    }
}

def sendPrivatMessage(String notifyChannel, SlackMessage message) {
    message.as_user = true
    sendMessage(notifyChannel, message)
}

@SuppressWarnings("GroovyAssignabilityCheck")
private static toJson(SlackMessage message) {
    def json = JsonOutput.toJson(message)
    // JsonOutput can be configured for this in groovy 2.5
    return json.replaceAll("(,)?\"(\\w*?)\":null", '').replaceAll("\\{,", '{')
}

def sendBuildStatus(String notifyChannel, String errorMessage = '') {
    SlackMessage message = new MessagesFactory(this).buildStatusMessage(errorMessage)
    sendMessage(notifyChannel, message)
}

def commitersOnly() {
    try {
        SlackMessage message = new MessagesFactory(this).buildStatusMessage()
        def commitAuthors = getCommitAuthors()
        commitAuthors.each {
            def slackUserId = getSlackUserIdByEmail(it)
            sendMessage(slackUserId, message)
        }
    } catch (e) {
        log.warn("Failed send Slack notification to the commit authors: " + e.toString())
    }
}

def prOwnerPrivateMessage(String url) {
    String prOwner = bitbucket.prOwnerEmail(url)
    SlackMessage message = new MessagesFactory(this).buildStatusMessage()
    def getUserFromSlackObject = getSlackUserIdByEmail(prOwner)
    sendPrivatMessage(getUserFromSlackObject, message)
}

def sendBuildStatusPrivateMessage(String slackUserId) {
    SlackMessage message = new MessagesFactory(this).buildStatusMessage()
    try {
        sendPrivatMessage(slackUserId, message)
    } catch (e) {
        log.warn("Failed send Slack notification to the authors: " + e.toString())
    }
}

def deployStart(String appName, String version, String environment, String notifyChannel) {
    def triggeredBy = getSlackUserIdByEmail(common.getCurrentUserEmail())
    SlackMessage message = new MessagesFactory(this).buildDeployStartMessage(appName, version, environment, triggeredBy)
    sendMessage(notifyChannel, message)
}

def deployFinish(String appName, String version, String environment, String notifyChannel) {
    SlackMessage message = new MessagesFactory(this).buildDeployFinishMessage(appName, version, environment)
    sendMessage(notifyChannel, message)
}

def getSlackUserIdByEmail(String userMail) {
    def response = httpRequest quiet: !log.isDebug(), consoleLogResponseBody: log.isDebug(), url: "${SLACK_URL}/api/users.lookupByEmail?token=${SLACK_BOT_TOKEN}&email=${userMail}"
    def responseJson = readJSON text: response.content

    if (responseJson.ok) {
        return responseJson.user.id
    } else {
        log.warn("\n\nUser mail in Slack ${userMail} doesn't match the one that is defined in Jenkins (LDAP) !!!\n\n return the default channel #testchannel <C9FRGVBPB>")
        def defaultId = 'C9FRGVBPB'
        return defaultId
    }
}
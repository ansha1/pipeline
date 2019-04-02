#!groovy
import java.net.URLEncoder
import static com.nextiva.SharedJobsStaticVars.*
import com.nextiva.slack.dto.SlackMessage
import com.nextiva.slack.MessagesFactory
import java.net.URLDecoder
import java.util.Random
import groovy.json.JsonOutput
import hudson.tasks.test.AbstractTestResultAction
import hudson.model.Actionable
import hudson.tasks.junit.CaseResult


@Deprecated
def call(String notifyChannel, def uploadSpec) {
    log.debug(uploadSpec)
    slackSend(channel: notifyChannel, attachments: uploadSpec, tokenCredentialId: "slackToken")
}

@SuppressWarnings("GroovyAssignabilityCheck")
def sendMessage(String notifyChannel, SlackMessage message) {
    message.setChannel(notifyChannel)
    log.info(message)
    httpRequest contentType: 'APPLICATION_JSON',
            quiet: false,
            consoleLogResponseBody: true,
            httpMode: 'POST',
            url: "https://nextivalab.slack.com/api/chat.postMessage",
            customHeaders:[[name:'Authorization', value:"Bearer ${SLACK_BOT_TOKEN}"]],
            requestBody: toJson(message)
}

@SuppressWarnings("GroovyAssignabilityCheck")
private static toJson(SlackMessage message) {
    def json = JsonOutput.toJson(message)
    //JsonOutput can be configured for this in groovy 2.5
    return json.replaceAll("(,)?\"(\\w*?)\":null", '').replaceAll("\\{,", '{')
}

def sendBuildStatus(String notifyChannel, String errorMessage = '') {
    SlackMessage message = new MessagesFactory(this).withError(errorMessage).buildStatusMessage()
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
    sendMessage(getUserFromSlackObject, message)
}

//def privateMessage(String slackUserId, String message) {
//    log.debug("Message: " + message)
//    def attachments = URLEncoder.encode(message, "UTF-8")
//    httpRequest contentType: 'APPLICATION_JSON', quiet: !log.isDebug(),
//            consoleLogResponseBody: log.isDebug(), httpMode: 'POST',
//            url: "https://nextivalab.slack.com/api/chat.postMessage?token=${SLACK_BOT_TOKEN}&channel=${slackUserId}&as_user=true&attachments=${attachments}"
//}
//
//def buildStatusMessageBody() {
//    def mention = ''
//    def buildStatus = currentBuild.currentResult
//    def commitInfoRaw = sh returnStdout: true, script: "git show --pretty=format:'The author was %an, %ar. Commit message: %s' | sed -n 1p"
//    def commitInfo = commitInfoRaw.trim()
//    if (buildStatus ==~ "FAILURE" && env.BRANCH_NAME ==~ /^(release\/.+|dev|master)$/) {
//        mention = "@here "
//    }
//    String jobName = URLDecoder.decode(env.JOB_NAME.toString(), 'UTF-8')
//    def subject = "Build status: ${buildStatus} Job: ${jobName} #${env.BUILD_ID}"
//    def uploadSpec = """[
//        {
//            "title": "${mention}${subject}",
//            "text": "${commitInfo}",
//            "color": "${SLACK_NOTIFY_COLORS.get(buildStatus)}",
//            "attachment_type": "default",
//            "actions": [
//                {
//                    "text": "Console output",
//                    "type": "button",
//                    "url": "${env.BUILD_URL}console"
//                },
//                {
//                    "text": "Test results",
//                    "type": "button",
//                    "url": "${env.BUILD_URL}testReport"
//                }
//            ]
//        }
//    ]"""
//    return uploadSpec
//}
//
//def buildAttachments(errorMessage = '') {
//    def mention = ''
//    def buildStatus = currentBuild.currentResult
//    if (buildStatus ==~ "FAILURE" && env.BRANCH_NAME ==~ /^(release\/.+|dev|master)$/) {
//        mention = "@here "
//    }
//    String jobName = URLDecoder.decode(env.JOB_NAME.toString(), 'UTF-8')
//    def subject = "Job: ${jobName}, build #${env.BUILD_NUMBER}"
//    def author = getGitAuthor()
//    log.info("author = ${author}")
//    def lastCommitMessage = getLastCommitMessage()
//    log.info("lastCommitMessage = ${lastCommitMessage}")
//
//    return JsonOutput.toJson([[
//                                      title      : "${mention}${subject}",
//                                      title_link : "${env.BUILD_URL}",
//                                      color      : "${SLACK_NOTIFY_COLORS.get(buildStatus)}",
//                                      author_name: "Commit autor: ${author}",
//                                      text       : "${errorMessage}",
//                                      fields     : [
//                                              [
//                                                      title: "Status",
//                                                      value: "${buildStatus}",
//                                                      short: true
//                                              ],
//                                              [
//                                                      title: "Branch",
//                                                      value: "${env.BRANCH_NAME}",
//                                                      short: true
//                                              ],
//                                              [
//                                                      title: "Test results",
//                                                      value: "getTestSummary()//",
//                                                      short: true
//                                              ],
//                                              [
//                                                      title: "Last Commit",
//                                                      value: "```${lastCommitMessage}```",
//                                                      short: false
//                                              ],
//                                      ],
//                                      "actions"  : [
//                                              [
//                                                      "text": "Console output",
//                                                      "type": "button",
//                                                      "url" : "${env.BUILD_URL}console"
//                                              ],
//                                              [
//                                                      "text": "Test results",
//                                                      "type": "button",
//                                                      "url" : "${env.BUILD_URL}testReport"
//                                              ]
//                                      ]
//
//                              ],
//    ])
//}
//
//def getGitAuthor() {
//    def commit = sh(returnStdout: true, script: 'git rev-parse HEAD')
//    return sh(returnStdout: true, script: "git --no-pager show -s --format='%an' ${commit}").trim()
//}
//
//def getLastCommitMessage() {
//    return sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
//}

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

//def getTestSummary() {
//    def testResultAction = currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
//    def summary
//
//    if (testResultAction != null) {
//        total = testResultAction.getTotalCount()
//        failed = testResultAction.getFailCount()
//        skipped = testResultAction.getSkipCount()
//
//        summary = "Passed: " + (total - failed - skipped)
//        summary = summary + (", Failed: " + failed)
//        summary = summary + (", Skipped: " + skipped)
//    } else {
//        summary = "No tests found"
//    }
//    return summary
//}

def sendBuildStatusPrivateMessage(String slackUserId) {
    SlackMessage message = new MessagesFactory(this).buildStatusMessage()
    try {
        sendMessage(slackUserId, message)
    } catch (e) {
        log.warn("Failed send Slack notification to the authors: " + e.toString())
    }
}

def deployStart(String appName, String version, String environment, String notifyChannel) {
    def triggeredBy = getSlackUserIdByEmail(common.getCurrentUserEmail())
    def message = "`${appName}:${version}` ${environment.toUpperCase()} deploy started by <@${triggeredBy}>"
    //TODO: this
    slackSend(channel: notifyChannel, color: SLACK_NOTIFY_COLORS.get(currentBuild.currentResult), message: message, tokenCredentialId: "slackToken")
}

def deployFinish(String appName, String version, String environment, String notifyChannel) {

    def wishList = libraryResource('wishes.txt').readLines()
    def randomWish = wishList[pickRandomNumber(wishList.size())]
    def buildStatus = currentBuild.currentResult == "SUCCESS" ? "deployed :tada: ${randomWish}" : "deploy failed! :disappointed:"

    def message = "`${appName}:${version}` ${environment.toUpperCase()} ${buildStatus}"
    //TODO: this
    slackSend(channel: notifyChannel, color: SLACK_NOTIFY_COLORS.get(currentBuild.currentResult), message: message, tokenCredentialId: "slackToken")
}

Integer pickRandomNumber(Integer max) {
    Random rand = new Random()
    return rand.nextInt(max)
}
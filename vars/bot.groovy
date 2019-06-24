import static com.nextiva.SharedJobsStaticVars.*


/**
 * Requests Bot to send approval request to Slack.
 *
 * @param slackReceiver - Slack user or channel identifier, should be of the format `<#C12345>`, `<@U12345>`, `<@U12345|user>`, `@user`, `#channel/user` or `#channel`
 * @param approveButtonText - Approval button text
 * @param declineButtonText - Decline button text
 * @param title - Title text
 * @param titleLink - Link for title text
 * @param text - Main text
 * @param jenkinsInputUrl - URL to Jenkins input form (example: https://jenkins.nextiva.xyz/jenkins/job/<job_name>/<build_id>/input/)
 * @param authorizedApprovers - List of usernames
 */
def getJenkinsApprove(String slackReceiver,
                      String approveButtonText,
                      String declineButtonText,
                      String title,
                      String titleLink,
                      String text,
                      String jenkinsInputUrl,
                      List authorizedApprovers = []) {

    log.debug("Slack receiver: " + slackReceiver)
    log.debug("Jenkins input URL: " + jenkinsInputUrl)
    log.debug("Message: " + text)

    def inputId = "${common.getRandomInt()}"
    def postBody = [slack_receiver: slackReceiver, yes_text: approveButtonText, no_text: declineButtonText, title: title,
                    title_link: titleLink, text: text, jenkins_input_url: jenkinsInputUrl + "${inputId}/submit"]

    httpRequest contentType: 'APPLICATION_JSON', quiet: !log.isDebug(),
            consoleLogResponseBody: log.isDebug(), httpMode: 'POST',
            url: JENKINS_BOT_URL + '/ask-question/', requestBody: groovy.json.JsonOutput.toJson(postBody)

    return input(id: inputId, message: text, ok: approveButtonText, submitter: authorizedApprovers.join(","))
}

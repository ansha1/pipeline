import static com.nextiva.SharedJobsStaticVars.*


/**
 * Requests Bot to send approval request to Slack.
 *
 * @param slackReceiver - Slack user or channel identifier (example: @user_name, @U6B2UE50Z or #channel_name)
 * @param yesText - Approval button text
 * @param noText - Decline button text
 * @param title - Title text
 * @param titleLink - Link for title text
 * @param text - Main text
 * @param jenkinsInputUrl - URL to Jenkins input form (example: https://jenkins.nextiva.xyz/jenkins/job/<job_name>/<build_id>/input/)
 * @param authorizedApprovers - List of usernames
 */
def getJenkinsApprove(String slackReceiver, String yesText, String noText, String title, String titleLink, String text,
                      String jenkinsInputUrl, List authorizedApprovers = []) {

    log.debug("Slack receiver: " + slackReceiver)
    log.debug("Jenkins input URL: " + jenkinsInputUrl)
    log.debug("Message: " + text)

    def inputId = "${common.getRundomInt()}"
    def postBody = [slack_receiver: slackReceiver, yes_text: yesText, no_text: noText, title: title,
                    title_link: titleLink, text: text, jenkins_input_url: jenkinsInputUrl + "${inputId}/submit"]

    httpRequest contentType: 'APPLICATION_JSON', quiet: !log.isDebug(),
            consoleLogResponseBody: log.isDebug(), httpMode: 'POST',
            url: JENKINS_BOT_URL + '/ask-question/', requestBody: groovy.json.JsonOutput.toJson(postBody)

    return input(id: inputId, message: text, ok: yesText, submitter: authorizedApprovers.join(","))
}

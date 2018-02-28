import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
def call(String slackChannel = '#testchannel') {
    JSONArray attachments = new JSONArray();
    JSONObject attachment = new JSONObject();
    attachment.put('text','I find your lack of faith disturbing!');
    attachment.put('fallback','Hey, Vader seems to be mad at you.');
    attachment.put('color','#ff0000');

    attachments.add(attachment);
//    buildStatus=currentBuild.currentResult


    test = '{\n' +
            '    "text": "<@W1A2BC3DD> approved your travel request. Book any airline you like by continuing below.",\n' +
            '    "channel": "C061EG9SL",\n' +
            '    "attachments": [\n' +
            '        {\n' +
            '            "fallback": "Book your flights at https://flights.example.com/book/r123456",\n' +
            '            "actions": [\n' +
            '                {\n' +
            '                    "type": "button",\n' +
            '                    "name": "travel_request_123456",\n' +
            '                    "text": "Book flights 🛫",\n' +
            '                    "url": "https://flights.example.com/book/r123456",\n' +
            '                    "style": "primary",\n' +
            '                    "confirm": "Really?"\n' +
            '                },\n' +
            '                {\n' +
            '                    "type": "button",\n' +
            '                    "name": "travel_cancel_123456",\n' +
            '                    "text": "Cancel travel request",\n' +
            '                    "url": "https://requests.example.com/cancel/r123456",\n' +
            '                    "style": "danger"\n' +
            '                }\n' +
            '            ]\n' +
            '        }\n' +
            '    ]\n' +
            '}'
    buildStatus='SUCCESS'
    notifyColor = ['SUCCESS': '#00FF00', 'FAILURE': '#FF0000', 'UNSTABLE': '#FF0000']

    def commitinfo = sh returnStdout: true, script: "git show --pretty=format:'The author was %an, %ar%nThe title was >>%s<<%n' | sed -n 1,2p"
    def subject = "Build status:${buildStatus} for job ${env.JOB_NAME} \n ${commitinfo}"

    def details = "Check console output at ${env.BUILD_URL}console\n" + commitinfo + "\n" + currentBuild.fullDisplayName + "\n" +
            "Test results: ${env.BUILD_URL}testReport"
    echo(notifyColor.get(buildStatus))
    slackSend (channel: "#testchannel", color: notifyColor.get(buildStatus), attachments: test, tokenCredentialId: "slackToken")
//    slackSend (channel: "#_devops_kyiv", color: notifyColor.get(buildStatus), message: subject.toString(), attachments: attachments.toString(), tokenCredentialId: "slackToken")
}


//
//{
//    "attachments": [
//        {
//            "fallback": "Required plain-text summary of the attachment.",
//            "color": "#36a64f",
//            "pretext": "Optional text that appears above the attachment block",
//            "author_name": "Bobby Tables",
//            "author_link": "http://flickr.com/bobby/",
//            "author_icon": "http://flickr.com/icons/bobby.jpg",
//            "title": "Slack API Documentation",
//            "title_link": "https://api.slack.com/",
//            "text": "Optional text that appears within the attachment",
//            "fields": [
//                {
//                    "title": "Priority",
//                    "value": "High",
//                    "short": false
//                }
//        ],
//            "image_url": "http://my-website.com/path/to/image.jpg",
//            "thumb_url": "http://example.com/path/to/thumb.png",
//            "footer": "Slack API",
//            "footer_icon": "https://platform.slack-edge.com/img/default_application_icon.png",
//            "ts": 123456789
//        }
//]
//}
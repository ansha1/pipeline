import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
def call(String slackChannel = '#testchannel') {
    JSONArray attachments = new JSONArray();
    JSONObject attachment = new JSONObject();
//    attachment.put('text','I find your lack of faith disturbing!');
//    attachment.put('fallback','Hey, Vader seems to be mad at you.');
//    attachment.put('color','#ff0000');

//    buildStatus=currentBuild.currentResult
//    def builder = new groovy.json.JsonBuilder()
//    def root = builder.people {
//attachment
    test = """
            {
                "text": "Would you like to play a game?",
                "attachments": [
                    {
                        "text": "Choose a game to play",
                        "fallback": "You are unable to choose a game",
                        "callback_id": "wopr_game",
                        "color": "#3AA3E3",
                        "attachment_type": "default",
                        "actions": [
                            {
                                "name": "game",
                                "text": "Chess",
                                "type": "button",
                                "value": "chess"
                            },
                            {
                                "name": "game",
                                "text": "Falken's Maze",
                                "type": "button",
                                "value": "maze"
                            },
                            {
                                "name": "game",
                                "text": "Thermonuclear War",
                                "style": "danger",
                                "type": "button",
                                "value": "war",
                                "confirm": {
                                "title": "Are you sure?",
                                "text": "Wouldn't you prefer a good game of chess?",
                                "ok_text": "Yes",
                                "dismiss_text": "No"
                            }
                            }
                    ]
                    }
            ]
            }
    """
    attachments.add(test);

    buildStatus='SUCCESS'
    notifyColor = ['SUCCESS': '#00FF00', 'FAILURE': '#FF0000', 'UNSTABLE': '#FF0000']

    def commitinfo = sh returnStdout: true, script: "git show --pretty=format:'The author was %an, %ar%nThe title was >>%s<<%n' | sed -n 1,2p"
    def subject = "Build status:${buildStatus} for job ${env.JOB_NAME} \n ${commitinfo}"

    def details = "Check console output at ${env.BUILD_URL}console\n" + commitinfo + "\n" + currentBuild.fullDisplayName + "\n" +
            "Test results: ${env.BUILD_URL}testReport"
    echo(notifyColor.get(buildStatus))
//    slackSend (channel: "#testchannel", color: notifyColor.get(buildStatus), attachments: test.toString(), tokenCredentialId: "slackToken")
    echo(attachments.toString())
    slackSend (channel: "#testchannel", color: notifyColor.get(buildStatus), attachments: attachments.toString(), tokenCredentialId: "slackToken")
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
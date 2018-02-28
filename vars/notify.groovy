def call(String slackChannel = '@evgeniy.sakhnyuk') {

    buildStatus=currentBuild.currentResult
    notifyColor = ['SUCCESS': '#00FF00', 'FAILURE': '#FF0000', 'UNSTABLE': '#FF0000']

    def subject = "${buildStatus}: Job '${env.JOB_NAME}' #${env.BUILD_NUMBER}"
//    def commitinfo = sh returnStdout: true, script: 'git show --pretty=format:"The author of %h was %an, %ar%nThe title was >>%s<<%n" | sed -n 1,2p'
    def commitinfo = env.CHANGE_AUTHOR_DISPLAY_NAME + env.CHANGE_AUTHOR_EMAIL + env.CHANGE_TARGET + env.CHANGE_URL + env.CHANGE_TITLE + env.CHANGE_AUTHOR
    def details = "Check console output at ${env.BUILD_URL}console\n" + commitinfo + "\n" + currentBuild.fullDisplayName + "\n" +
            "Test results: ${env.BUILD_URL}testReport"
    slackSend channel: slackChannel, color: notifyColor.get(buildStatus), message: subject + ' ' + details, tokenCredentialId: 'slackToken'
}
def call(String slackChannel= '#devops_kyiv') {
    echo '1'
    buildStatus=currentBuild.currentResult
    notifyColor = ['SUCCESS': '#00FF00', 'FAILURE': '#FF0000', 'UNSTABLE': '#FF0000']
    echo '2'
    def subject = "${buildStatus}: Job '${env.JOB_NAME}' #${env.BUILD_NUMBER}"
    def commitinfo = sh returnStdout: true, script: 'git show --pretty=format:"The author of %h was %an, %ar%nThe title was >>%s<<%n" | sed -n 1,2p'
    def details = "Check console output at ${env.BUILD_URL}console\n" + commitinfo + "\n" + currentBuild.fullDisplayName + "\n" +
            "Test results: ${env.BUILD_URL}testReport"
    echo '3'
    slackSend channel: slackChannel, color: notifyColor.get(buildStatus), message: subject + ' ' + details, tokenCredentialId: 'slackToken'
}
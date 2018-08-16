def call(String notifyChannel) {
    log.deprecated('Use slack.sendBuildStatus() method.')
    currentBuild.rawBuild.result = Result.UNSTABLE
    slack.sendBuildStatus(notifyChannel)
}

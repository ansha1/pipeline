def call(String notifyChannel) {
    log.warning('DEPRECATED: Use slack.sendBuildStatus() method.')
    currentBuild.rawBuild.result = Result.UNSTABLE
    slack.sendBuildStatus(notifyChannel)
}

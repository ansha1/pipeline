def call(String notifyChannel) {
    log.warning('DEPRECATED: Use slack.sendBuildStatus() method.')
    slack.sendBuildStatus(notifyChannel)
}

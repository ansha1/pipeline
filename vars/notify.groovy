def call(String notifyChannel) {
    log.warning('DEPRECATED: Use slackNotify() method.')
    slackNotify(notifyChannel)
}

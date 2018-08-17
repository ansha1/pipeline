def call(String notifyChannel) {
    log.deprecated('Use slack.sendBuildStatus() method.')
    slack.sendBuildStatus(notifyChannel)
}

def commitersOnly() {
    log.deprecated('Use slack.commitersOnly() method.')
    slack.commitersOnly()
}

def prOwnerPrivateMessage(String url) {
    log.deprecated('Use slack.prOwnerPrivateMessage() method.')
    slack.prOwnerPrivateMessage(url)
}

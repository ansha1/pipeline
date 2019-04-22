@Deprecated
def call(String notifyChannel) {
    log.deprecated('Use slack.sendBuildStatus() method.')
    slack.sendBuildStatus(notifyChannel)
}

@Deprecated
def commitersOnly() {
    log.deprecated('Use slack.commitersOnly() method.')
    slack.commitersOnly()
}

@Deprecated
def prOwnerPrivateMessage(String url) {
    log.deprecated('Use slack.prOwnerPrivateMessage() method.')
    slack.prOwnerPrivateMessage(url)
}

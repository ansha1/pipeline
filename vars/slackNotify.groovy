def call(String notifyChannel) {
    log.warning('DEPRECATED: Use slack.sendBuildStatus() method.')
    slack.sendBuildStatus(channel: notifyChannel, attachments: uploadSpec, tokenCredentialId: "slackToken")
}

def commitersOnly() {
    log.warning('DEPRECATED: Use slack.commitersOnly() method.')
    slack.commitersOnly()
}

def prOwnerPrivateMessage(String url) {
    log.warning('DEPRECATED: Use slack.prOwnerPrivateMessage() method.')
    slack.prOwnerPrivateMessage(url)
}

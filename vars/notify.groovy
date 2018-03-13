def call(String notifyChannel) {
    echo('DEPRECATED, use slackNotify() method')
    slackNotify(notifyChannel)
}
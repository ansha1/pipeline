def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def User = config.get('user')
    return User.getProperty(hudson.tasks.Mailer.UserProperty.class).getAddress()
}
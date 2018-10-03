import static com.nextiva.SharedJobsStaticVars.*

def getCurrentUserLogin() {
    def build = currentBuild.rawBuild
    def cause = build.getCause(hudson.model.Cause.UserIdCause.class)

    if (!cause) {
        return null
    }

    return cause.getUserId()
}

def getCurrentUser() {
    String userId = getCurrentUserLogin()
    def user = User.get(userId)

    return user
}

def getCurrentUserEmail() {
    try {
        def user = getCurrentUser()
        def umail = user.getProperty(hudson.tasks.Mailer.UserProperty.class)
        return umail.getAddress()
    } catch (e) {
        log.warn("Error in getCurrentUserEmail() ${e}")
        return null
    }
}

def getCurrentUserSlackId() {
    String userEmail = getCurrentUserEmail()
    return slack.getSlackUserIdByEmail(userEmail)
}

String getAppNameFromGitUrl(String gitUrl) {
    return gitUrl.split("/")[-1].replaceAll('.git', '')
}

def getPropertyValue(String propertyName, def defaultValue = null) {
    try {
        return getProperty(propertyName)
    } catch (MissingPropertyException e) {
        return defaultValue
    }
}

def getPropertyBooleanValue(String propertyName, boolean defaultValue) {
    return new Boolean(getPropertyValue(propertyName, defaultValue))
}

def remoteSh(String hostname, String cmd) {
    sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
        sh """ssh -o StrictHostKeyChecking=no ${hostname} ' uname -a'
              ssh ${hostname} '${cmd}'
        """
    }
}

def serviceStart(String hostname, String service) {
    remoteSh(hostname, "sudo systemctl start ${service}")
}

def serviceStop(String hostname, String service) {
    remoteSh(hostname, "sudo systemctl stop ${service}")
}

def serviceRestart(String hostname, String service) {
    remoteSh(hostname, "sudo systemctl restart ${service}")
}

def serviceStatus(String hostname, String service) {
    remoteSh(hostname, "sudo systemctl status -l ${service} || true")
}

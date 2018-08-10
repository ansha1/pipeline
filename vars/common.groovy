def getCurrentUserLogin() {
    def build = currentBuild.rawBuild
    def cause = build.getCause(hudson.model.Cause.UserIdCause.class)

    if( !cause ){
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
    def user = getCurrentUser()
    def umail = user.getProperty(hudson.tasks.Mailer.UserProperty.class)
    return umail.getAddress()
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

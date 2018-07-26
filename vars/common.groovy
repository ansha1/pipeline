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
    log.info('userId: ' + userId)
    User user = User.get(userId)
    log.info('user object: '+ user)
    return user
}

def getCurrentUserEmail() {
    def user = getCurrentUser()
    def umail = user.getProperty(Mailer.UserProperty.class)
    return umail.getAddress()
}

def getCurrentUserSlackId() {
    String userEmail = getCurrentUserEmail()
    return slack.getSlackUserIdByEmail(userEmail)
}

String getAppNameFromGitUrl(String gitUrl) {
    return gitUrl.split("/")[-1].replaceAll('.git', '')
}

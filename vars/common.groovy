def getCurrentUser() {
    build = currentBuild.rawBuild
    cause = build.getCause(hudson.model.Cause.UserIdCause.class)

    if( !cause ){
        return null
    }
    
    String userId = cause.getUserId()
    User user = User.get(userId)   
    //print userId  // login
    //print user  // user full name
    //def umail = user.getProperty(Mailer.UserProperty.class)
    //print umail.getAddress()  //email

    return user
}

def getCurrentUserEmail() {
    def user = getCurrentUser()
    def umail = user.getProperty(Mailer.UserProperty.class)
    return umail.getAddress()
}

def getCurrentUserName() {
    def build = currentBuild.rawBuild
    def cause = build.getCause(hudson.model.Cause.UserIdCause.class)
    String userId = ""

    if(cause != null){
        userId = cause.getUserId()
    }
    User user = User.get(userId)   
    //print userId  // login
    //print user  // user full name
    //def umail = user.getProperty(Mailer.UserProperty.class)
    //print umail.getAddress()  //email

    return "${user}"
}

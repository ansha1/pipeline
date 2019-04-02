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

def getRundomInt() {
    log.deprecated('Use getRandomInt() method.')
    return getRandomInt()
}

def getRandomInt() {
    return System.nanoTime()
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

def tempDir(path = '', closure) {
    path = path ?: "tmp_${getRandomInt()}"
    log.debug("Using temporary dir: ${path}")

    dir(path) {
        closure()
        deleteDir()
    }
}

String getRepositoryUrl() {
    def repositoryUrl =  sh returnStdout: true, script: "git config --get remote.origin.url"
    return repositoryUrl.trim()
}

String getCurrentCommit() {
    def currentCommit = sh returnStdout: true, script: 'git rev-parse HEAD'
    return currentCommit.trim()
}

// Load groovy script on runtime
// example: sharedComponents = loadScript("aws/sharedComponents.groovy")
def loadScript(String scriptPath, String nodeLabel = 'master') {
    def script
    node(nodeLabel) {
        cleanWs()
        checkout scm
        script = load scriptPath
    }
    return script
}

String getRepositoryProjectKeyFromUrl(String repositoryUrl) {
    log.debug("Repository Url: ${repositoryUrl}")

    String tokens = repositoryUrl.tokenize('/')
    if (tokens[0] == 'https:') {
        // example: https://git.nextiva.xyz/scm/REL/k8s-platform.git
        return tokens[3]
    }
    else {
        // example: ssh://git@git.nextiva.xyz:7999/REL/k8s-platform.git
        return tokens[2]
    }
}

String getRepositoryNameFromUrl(String repositoryUrl) {
    log.debug("Repository Url: ${repositoryUrl}")

    String tokens = repositoryUrl.tokenize('/')
    if (tokens[0] == 'https:') {
        // example: https://git.nextiva.xyz/scm/REL/k8s-platform.git
        return tokens[4].replace('.git', '')
    }
    else {
        // example: ssh://git@git.nextiva.xyz:7999/REL/k8s-platform.git
        return tokens[3].replace('.git', '')
    }
}

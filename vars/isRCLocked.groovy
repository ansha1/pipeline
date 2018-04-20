import static com.nextiva.SharedJobsStaticVars.*

def call(String jobName = 'RCJobsLock') {
    response = httpRequest authentication: JENKINS_AUTH_CREDENTIALS, quiet: true, url: "${JENKINS_URL}job/${jobName}/api/json"
    def responsebody = readJSON text: response.content;
    boolean result = responsebody.buildable

    return result
}

def checkState() {
    // check if RC in locked state
    if (env.BRANCH_NAME ==~ ~/^release\/.+$/ && isRCLocked()) {
        currentBuild.rawBuild.result = Result.ABORTED
        throw new hudson.AbortException("\nAll RC deploy jobs are locked !!!\nPlease contact QA Core Team.\n")
    }
}

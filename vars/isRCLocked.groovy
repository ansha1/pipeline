import static com.nextiva.SharedJobsStaticVars.*

def call(String jobName = 'RCJobsLock') {
    response = httpRequest authentication: JENKINS_AUTH_CREDENTIALS, quiet: true, url: "${JENKINS_URL}job/${jobName}/api/json"
    def responsebody = readJSON text: response.content;
    boolean result = responsebody.buildable

    return result
}
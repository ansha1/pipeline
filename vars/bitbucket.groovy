#!groovy
import static com.nextiva.SharedJobsStaticVars.*

def prOwnerEmail(String url) {
    def pr = getPrFromUrl(url)
    return pr.author.user.emailAddress
}

def getPrFromUrl(String url) {

    log("Received PR url: ${url}")
    prUrl = url.replaceAll("git.nextiva.xyz/projects", "git.nextiva.xyz/rest/api/1.0/projects") - "/overview"
    log("Transform Url for access via rest api: ${prUrl}")

    def prResponce = httpRequest authentication: BITBUCKET_JENKINS_AUTH, httpMode: 'GET', url: prUrl
    def returnBody = readJSON text: prResponce.content
    return returnBody
}

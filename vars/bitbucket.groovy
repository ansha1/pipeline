def prOwnerEmail(String changeUrl) {
    def pr = getPrFromUrl(changeUrl)
    return pr.author.user.emailAddress
}

def getPrFromUrl(Sting url) {

    log("Received PR url: ${url}")
    prUrl = url.replaceAll("git.nextiva.xyz/projects", "git.nextiva.xyz/rest/api/1.0/projects") - "/overview"
    log("Transform Url for access via rest api: ${prUrl}")

    def prResponce = httpRequest authentication: BITBUCKET_JENKINS_AUTH, httpMode: 'GET', url: prUrl
    return readJSON text: prResponce.content
}
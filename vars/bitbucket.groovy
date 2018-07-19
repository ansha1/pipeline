def getPrFromUrl(Sting url) {

    log("Received PR url: ${url}")
    prUrl = url.replaceAll("git.nextiva.xyz/projects", "git.nextiva.xyz/rest/api/1.0/projects") - "/overview"
    log("Transform Url for access via rest api: ${prUrl}")

    def prResponce = httpRequest authentication: BITBUCKET_JENKINS_AUTH, httpMode: 'GET', url: prUrl
    return readJSON text: prResponce.content
}

def prOwnerEmail() {
    def pr = bitbucket.getPrFromUrl(env.CHANGE_URL)
    return pr.author.user.emailAddress
}
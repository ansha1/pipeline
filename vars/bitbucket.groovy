#!groovy
import static com.nextiva.SharedJobsStaticVars.*
import groovy.json.JsonOutput

String getSourceBranchFromPr(String url) {

    def props = getPrFromUrl(url)
    def sourceBranch = props.fromRef.displayId.trim()
    log.info("SourceBranch: ${sourceBranch}")
    return sourceBranch
}

String getDestinationBranchFromPr(String url) {

    def props = getPrFromUrl(url)
    def destinationBranch = props.toRef.displayId.trim()
    log.info("DestinationBranch: ${destinationBranch}")
    return destinationBranch
}

def prOwnerEmail(String url) {
    def pr = getPrFromUrl(url)
    return pr.author.user.emailAddress
}

def getPrFromUrl(String url) {

    log.info("Received PR url: ${url}")
    prUrl = url.replaceAll("${BITBUCKET_URL}/projects", "${BITBUCKET_URL}/rest/api/1.0/projects") - "/overview"
    log.info("Transform Url for access via rest api: ${prUrl}")

    def prResponce = httpRequest authentication: BITBUCKET_JENKINS_AUTH, httpMode: 'GET', url: prUrl,
         consoleLogResponseBody: log.isDebug()
    def returnBody = readJSON text: prResponce.content
    return returnBody
}

def createPr(String repositoryUrl, String sourceBranch, String destinationBranch, String title, String description) {

    def tokens = repositoryUrl.tokenize('/')
    def projectKey = tokens[2]
    def repositorySlug = tokens[3].replace(".git", "")
    log.info('projectKey: ' + projectKey)
    log.info('repositorySlug: ' + repositorySlug)

    String reviewersUrl = "${BITBUCKET_URL}/rest/default-reviewers/1.0/projects/${projectKey}/repos/${repositorySlug}/conditions"
    String createPrUrl = "${BITBUCKET_URL}/rest/api/1.0/projects/${projectKey}/repos/${repositorySlug}/pull-requests"

    //Getting default reviewers list from target repo
    def reviewersResponce = httpRequest authentication: BITBUCKET_JENKINS_AUTH, httpMode: 'GET', url: reviewersUrl,
         consoleLogResponseBody: log.isDebug()
    def props = readJSON text: reviewersResponce.content
    def revs = props[0].reviewers
    log.info("Get reviewers")
    def revsList = []
    revs.each { revsList.add(['user': ['name': it.name]]) }
    def reviewers = JsonOutput.toJson(revsList)

    //Creating pull request via Bitbucket API
    def requestBody = """{
                    "title": "${title}",
                    "description": "${description}",
                    "state": "OPEN",
                    "open": true,
                    "closed": false,
                    "fromRef": {
                        "id": "${sourceBranch}"
                    },
                    "toRef": {
                        "id": "${destinationBranch}"
                    },
                    "locked": false,
                    "reviewers": ${reviewers},
                    "links": {
                        "self": [
                                null
                        ]
                    }
                }"""
    def pullRequestResponce = httpRequest authentication: BITBUCKET_JENKINS_AUTH, contentType: 'APPLICATION_JSON', httpMode: 'POST', requestBody: requestBody, url: createPrUrl,
         consoleLogResponseBody: log.isDebug()
    def responceJson = readJSON text: pullRequestResponce.content
    String pullRequestLink = responceJson.links.self[0].get('href')
    log.info("PULL REQUEST WAS CREATED ${pullRequestLink}")
    return pullRequestLink
}

List<String> getChangesFromPr(String repositoryUrl, String prID, String startPage = 0, String limit = 1000) {

    def tokens = repositoryUrl.tokenize('/')
    def projectKey = tokens[2]
    def repositorySlug = tokens[3].replace(".git", "")

    String getChangesUrl = "${BITBUCKET_URL}/rest/api/latest/projects/${projectKey}/repos/${repositorySlug}" +
            "/pull-requests/${prID}/changes?start=${startPage}&limit=${limit}"

    def changesResponse = httpRequest authentication: BITBUCKET_JENKINS_AUTH, httpMode: 'GET', url: getChangesUrl,
            consoleLogResponseBody: log.isDebug()
    def changesResponseJson = readJSON text: changesResponse.content

    def changedFiles = []

    changesResponseJson.values.each {
        changedFiles << it.path.toString
        it?.srcPath?.toString && changedFiles << it.srcPath.toString
    }

    return changedFiles
}

def updatePrDescriptionSection(String repositoryUrl, String prID, String sectionBody, String sectionTag) {

    def tokens = repositoryUrl.tokenize('/')
    def projectKey = tokens[2]
    def repositorySlug = tokens[3].replace(".git", "")

    String prUrl = "${BITBUCKET_URL}/rest/api/latest/projects/${projectKey}/repos/${repositorySlug}/pull-requests/${prID}"

    def response = httpRequest authentication: BITBUCKET_JENKINS_AUTH, httpMode: 'GET', url: prUrl,
            consoleLogResponseBody: log.isDebug()
    def originalPr = readJSON text: response.content

    def originalDescription = parseDescription(originalPr.description as String)

    originalDescription.put(sectionTag, sectionBody)

    def updatedDescription = descriptionToString(originalDescription)

    def updatedPr = [:]
    updatedPr.description = updatedDescription
    updatedPr.version = originalPr.version
    updatedPr.reviewers = originalPr.reviewers

    log.info(updatedPr)

    httpRequest authentication: BITBUCKET_JENKINS_AUTH,
            contentType: 'APPLICATION_JSON',
            quiet: !log.isDebug(),
            consoleLogResponseBody: true,
            httpMode: 'PUT',
            url: prUrl,
            requestBody: JsonOutput.toJson(updatedPr)

}

static Map<String, String> parseDescription(String description) {
    return new HashMap<String, String>()
}

static String descriptionToString(Map<String, String> description) {
    return "Kappa123"
}
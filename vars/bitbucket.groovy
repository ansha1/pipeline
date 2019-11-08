#!groovy
import com.nextiva.bitbucket.Bitbucket
import com.nextiva.bitbucket.BitbucketCloud
import com.nextiva.bitbucket.BitbucketServer

Bitbucket getClientForUrl(String url) {
    Bitbucket client = url.contains('bitbucket.org')
            ? new BitbucketCloud(this)
            : new BitbucketServer(this)

    String name = client.getClass().getSimpleName()
    log.info("Created Client: ${name}")
    return client
}

String getTitleFromPr(String url) {
    Bitbucket client = getClientForUrl(url)
    Map pr = client.getPullRequestFromUrl(url)
    return client.getPullRequestTitle(pr)
}

String getSourceBranchFromPr(String url) {
    Bitbucket client = getClientForUrl(url)
    Map pr = client.getPullRequestFromUrl(url)
    return client.getPullRequestSourceBranch(pr)
}

String getDestinationBranchFromPr(String url) {
    Bitbucket client = getClientForUrl(url)
    Map pr = client.getPullRequestFromUrl(url)
    return client.getPullRequestDestinationBranch(pr)
}

String prOwnerEmail(String url) {
    Bitbucket client = getClientForUrl(url)
    Map pr = client.getPullRequestFromUrl(url)
    return client.getPullRequestAuthorEmail(pr)
}

Map getPrFromUrl(String url) {
    Bitbucket client = getClientForUrl(url)
    return client.getPullRequestFromUrl(url)
}

String getProjectKeyFromUrl(String repositoryUrl) {
    Bitbucket client = getClientForUrl(repositoryUrl)
    return client.getProjectKeyFromUrl(repositoryUrl)
}

String getRepositoryNameFromUrl(String repositoryUrl) {
    Bitbucket client = getClientForUrl(repositoryUrl)
    return client.getRepositoryNameFromUrl(repositoryUrl)
}

def createPr(String repositoryUrl, String sourceBranch, String destinationBranch, String title, String description) {
    Bitbucket client = getClientForUrl(repositoryUrl)
    String repository = client.getRepositoryNameFromUrl(repositoryUrl)
    String project = client.getProjectKeyFromUrl(repositoryUrl)
    client.createPullRequest(repository, sourceBranch, destinationBranch, title, description, project)
}

List<String> getChangesFromPr(String repositoryUrl, String prID, String startPage = 0, String limit = 1000) {
    Bitbucket client = getClientForUrl(repositoryUrl)
    String repository = client.getRepositoryNameFromUrl(repositoryUrl)
    String project = client.getProjectKeyFromUrl(repositoryUrl)
    int pullRequest = prID.toInteger()
    Map pr = client.getPullRequest(repository, pullRequest, project)
    return client.getPullRequestChangedFiles(pr)
}

def updatePrDescriptionSection(String repositoryUrl, String prID, String sectionTag, String sectionBody) {
    int pullRequest = prID.toInteger()
    String shortbody = sectionBody.length() > 20
            ? "${sectionBody.take(10)}...${sectionBody[-10..-1]}"
            : sectionBody

    log.info("Update Pull Request Description Section")
    log.info("REPOSITORY URL: ${repositoryUrl}")
    log.info("PULL REQUEST: ${pullRequest}")
    log.info("SECTION: ${sectionTag}")
    log.info("BODY: ${shortbody}")

    Bitbucket client = getClientForUrl(repositoryUrl)
    String repository = client.getRepositoryNameFromUrl(repositoryUrl)
    String project = client.getProjectKeyFromUrl(repositoryUrl)

    log.info("REPOSITORY SLUG: ${repository}")
    log.info("PROJECT KEY: ${project}")

    Map pr = client.getPullRequest(repository, pullRequest, project)
    log.info("Found Pull Request Data")

    client.updatePullRequestDescriptionSection(pr, sectionTag, sectionBody)
    log.info("Description Updated")
}

Map parseDescription(String description) {
    return Bitbucket.parseDescription(description)
}

String descriptionToString(Map<String, String> description) {
    return Bitbucket.descriptionToString(description)
}

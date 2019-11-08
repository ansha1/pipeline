package com.nextiva.bitbucket

import groovy.json.JsonOutput

import static com.nextiva.SharedJobsStaticVars.BITBUCKET_JENKINS_AUTH
import static com.nextiva.SharedJobsStaticVars.BITBUCKET_URL

class BitbucketServer extends Bitbucket {

    def log

    BitbucketServer(context) {
        super(context)
        log = context.log
    }

    @Override
    Map getPullRequest(String repository, int pullRequest, String project) {
        String prUrl = "${BITBUCKET_URL}/rest/api/latest/projects/${project}/repos/${repository}/pull-requests/${pullRequest}"

        def response = getPipelineContext().httpRequest(
                authentication: BITBUCKET_JENKINS_AUTH,
                httpMode: 'GET',
                url: prUrl,
                consoleLogResponseBody: log.isDebug()
        )

        return getPipelineContext().readJSON(text: response.content)
    }

    @Override
    Map getPullRequestFromUrl(String url) {
        log.info("Received PR url: ${url}")
        def prUrl = url.replaceAll("${BITBUCKET_URL}/projects", "${BITBUCKET_URL}/rest/api/1.0/projects") - "/overview"
        log.info("Transform Url for access via rest api: ${prUrl}")

        def prResponce = getPipelineContext().httpRequest authentication: BITBUCKET_JENKINS_AUTH,
                httpMode: 'GET',
                url: prUrl,
                consoleLogResponseBody: log.isDebug()
        def returnBody = getPipelineContext().readJSON text: prResponce.content
        return returnBody
    }

    @Override
    String getPullRequestAuthorEmail(Map pr) {
        return pr.author.user.emailAddress
    }

    @Override
    String getPullRequestDestinationBranch(Map pr) {
        def destinationBranch = pr.toRef.displayId.trim()
        log.info("DestinationBranch: ${destinationBranch}")
        return destinationBranch
    }

    @Override
    String getPullRequestSourceBranch(Map pr) {
        def sourceBranch = pr.fromRef.displayId.trim()
        log.info("SourceBranch: ${sourceBranch}")
        return sourceBranch
    }

    @Override
    String getPullRequestTitle(Map pr) {
        def prTitle = pr.title.trim()
        log.info("PR title: ${prTitle}")
        return prTitle
    }

    @Override
    String getProjectKeyFromUrl(String url) {
        log.info("Repository Url: ${url}")

        List tokens = url.tokenize('/')
        if (tokens[0] == 'https:') {
            // example: https://git.nextiva.xyz/scm/cloud/cloud-apps.git
            return tokens[3]
        } else {
            // example: ssh://git@git.nextiva.xyz:7999/cloud/cloud-apps.git
            return tokens[2]
        }
    }

    @Override
    String getRepositoryNameFromUrl(String repositoryUrl) {
        log.info("Repository Url: ${repositoryUrl}")

        List tokens = repositoryUrl.tokenize('/')
        if (tokens[0] == 'https:') {
            // example: https://git.nextiva.xyz/scm/cloud/cloud-apps.git
            return tokens[4].replace('.git', '')
        } else {
            // example: ssh://git@git.nextiva.xyz:7999/cloud/cloud-apps.git
            return tokens[3].replace('.git', '')
        }
    }

    @Override
    def getRepositoryDefaultReviewers(String repository, String project) {
        String uri = "${BITBUCKET_URL}/rest/default-reviewers/1.0/projects/${project}/repos/${repository}/conditions"
        def reviewersResponce = getPipelineContext().httpRequest authentication: BITBUCKET_JENKINS_AUTH, httpMode: 'GET', url: uri,
                consoleLogResponseBody: log.isDebug()
        def props = getPipelineContext().readJSON(text: reviewersResponce.content)
        def revs = props[0].reviewers
        log.info("Get reviewers")
        def revsList = []
        revs.each { revsList.add(['user': ['name': it.name]]) }
        return JsonOutput.toJson(revsList)
    }

    @Override
    String createPullRequest(
            String repositorySlug,
            String sourceBranch,
            String destinationBranch,
            String title,
            String description,
            String projectKey
    ) {

        log.info('projectKey: ' + projectKey)
        log.info('repositorySlug: ' + repositorySlug)

        String createPrUrl = "${BITBUCKET_URL}/rest/api/1.0/projects/${projectKey}/repos/${repositorySlug}/pull-requests"

        def reviewers = getRepositoryDefaultReviewers(repositorySlug, projectKey)

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
        def pullRequestResponce = getPipelineContext().httpRequest authentication: BITBUCKET_JENKINS_AUTH,
                contentType: 'APPLICATION_JSON', httpMode: 'POST', requestBody: requestBody, url: createPrUrl,
                consoleLogResponseBody: log.isDebug()
        def responceJson = getPipelineContext().readJSON text: pullRequestResponce.content
        String pullRequestLink = responceJson.links.self[0].get('href')
        log.info("PULL REQUEST WAS CREATED ${pullRequestLink}")
        return pullRequestLink
    }

    /**
     * {@inheritDoc}
     */
    @Override
    List<String> getPullRequestChangedFiles(Map pr) {
        String repositorySlug = pr['toRef']['repository']['slug']
        String projectKey = pr['toRef']['repository']['project']['key']
        int prID = pr['id'].toString().toInteger()

        String getChangesUrl = "${BITBUCKET_URL}/rest/api/latest/projects/${projectKey}/repos/${repositorySlug}" +
                "/pull-requests/${prID}/changes?start=0&limit=1000"

        def changesResponse = getPipelineContext().httpRequest(
                authentication: BITBUCKET_JENKINS_AUTH,
                httpMode: 'GET',
                url: getChangesUrl,
                consoleLogResponseBody: log.isDebug()
        )

        def changesResponseJson = getPipelineContext().readJSON(text: changesResponse.content)

        def changedFiles = []

        changesResponseJson.values.each {
            changedFiles << it.path.toString
            it?.srcPath?.toString && changedFiles << it.srcPath.toString.trim()
        }

        return changedFiles
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void updatePullRequestDescriptionSection(Map pr, String section, String body) {
        String repositorySlug = pr['toRef']['repository']['slug']
        String projectKey = pr['toRef']['repository']['project']['key']
        int prID = pr['id'].toString().toInteger()

        String prUrl = "${BITBUCKET_URL}/rest/api/latest/projects/${projectKey}/repos/${repositorySlug}/pull-requests/${prID}"

        def response = getPipelineContext().httpRequest(
                authentication: BITBUCKET_JENKINS_AUTH,
                httpMode: 'GET',
                url: prUrl,
                consoleLogResponseBody: log.isDebug()
        )

        def originalPr = getPipelineContext().readJSON(text: response.content)
        def originalDescription = parseDescription(originalPr.description.toString())

        originalDescription.put(section, body)

        def updatedDescription = descriptionToString(originalDescription)

        def updatedPr = [:]
        updatedPr.description = updatedDescription
        updatedPr.version = originalPr.version
        updatedPr.reviewers = originalPr.reviewers

        getPipelineContext().httpRequest(
                authentication: BITBUCKET_JENKINS_AUTH,
                contentType: 'APPLICATION_JSON',
                quiet: !log.isDebug(),
                consoleLogResponseBody: log.isDebug(),
                httpMode: 'PUT',
                url: prUrl,
                requestBody: JsonOutput.toJson(updatedPr)
        )
    }
}

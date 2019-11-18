package com.nextiva.bitbucket

import groovy.json.JsonOutput

import java.util.regex.Matcher
import java.util.regex.Pattern

/*
 * Copyright (c) 2019 Nextiva, Inc. to Present.
 * All rights reserved.
 */

class BitbucketCloud extends Bitbucket {

    def log

    String credentialsId = 'bitbucket-cloud-api'
    String owner = 'nextiva'

    Pattern prIdRegex = Pattern.compile("^.*/pull-requests/(\\d+).*\$", Pattern.CASE_INSENSITIVE)
    Pattern repositoryRegex = Pattern.compile("^[^\\s]+${owner}/([^/\\s]+)[^\\s]*\$", Pattern.CASE_INSENSITIVE)

    BitbucketCloud(context) {
        super(context)
        log = context.log
    }

    /**
     * @param diff the git diff file
     * @return list of affected files
     */
    private List<String> getFileListFromDiff(String diff) {
        if (!diff?.trim()) {
            return []
        }

        return diff.tokenize("\n")
                .findAll { it.startsWith("diff --git") }
                .collect { it =~ /^diff --git a\/(.*) b\/(.*)/ }
                .collect { [it.group(1), it.group(2)] }
                .flatten()
                .unique()
                .collect { it.toString() }
    }

    /**
     * @param repository the name of the repository
     * @param pr the PR information
     * @return git diff of PR
     */
    private String getPullRequestDiff(Map pr) {
        String repository = pr['destination']['repository']['name']
        String src = pr['source']['commit']['hash']
        String dest = pr['destination']['commit']['hash']

        String uri = "https://bitbucket.org/api/2.0/repositories/${owner}/${repository}/diff/${src}..${dest}"
        def response = getPipelineContext().httpRequest(
                authentication: credentialsId,
                httpMode: 'GET',
                url: uri,
                validResponseCodes: '200,555',
                consoleLogResponseBody: log.isDebug()
        )

        return response.status == 555 ? '' : response.content
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Map getPullRequest(String repository, int pullRequest, String project = owner) {
        String uri = "https://bitbucket.org/api/2.0/repositories/${owner}/${repository}/pullrequests/${pullRequest}"
        log.info("Transform Url for access via rest api: ${uri}")
        def response = getPipelineContext().httpRequest(
                authentication: credentialsId,
                httpMode: 'GET',
                url: uri,
                consoleLogResponseBody: log.isDebug()
        )

        return getPipelineContext().readJSON(text: response.content)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getProjectKeyFromUrl(String url) {
        return owner
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getRepositoryNameFromUrl(String url) {
        if (!url?.trim()) {
            return ""
        }

        Matcher m = repositoryRegex.matcher(url.replace('.git', ''))
        return m.matches() ? m.group(1) : ""
    }

    /**
     * @param url the event notification url
     * @return pull request id
     */
    int getPullRequestIdFromUrl(String url) {
        if (!url?.trim()) {
            return 0
        }

        Matcher m = prIdRegex.matcher(url)
        return m.matches() ? m.group(1).toInteger() : 0
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Map getPullRequestFromUrl(String url) {
        log.info("Received PR url: ${url}")

        String repository = getRepositoryNameFromUrl(url)
        int pullRequest = getPullRequestIdFromUrl(url)

        if (repository?.trim() && pullRequest > 0) {
            return getPullRequest(repository, pullRequest)
        }

        return [:]
    }

    /**
     * Since cloud doesn't provide the email in the response, this method guesses the user email based on our
     * naming pattern for email addresses
     * @return hopefully a valid email address
     */
    @Override
    String getPullRequestAuthorEmail(Map pr) {
        String author = pr['author']['display_name'].toString().toLowerCase()
        String[] tokens = author.split("\\s")
        return tokens.length == 2
                ? "${tokens[0]}.${tokens[1]}@nextiva.com"
                : ""
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getPullRequestDestinationBranch(Map pr) {
        if (pr.isEmpty()) {
            return ""
        }

        String sourceBranch = pr['destination']['branch']['name']
        log.info("DestinationBranch: ${sourceBranch}")
        return sourceBranch
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getPullRequestSourceBranch(Map pr) {
        if (pr.isEmpty()) {
            return ""
        }

        String sourceBranch = pr['source']['branch']['name']
        log.info("SourceBranch: ${sourceBranch}")
        return sourceBranch
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getPullRequestTitle(Map pr) {
        if (pr.isEmpty()) {
            return ""
        }

        String title = pr['title'].trim()
        log.info("PR title: ${title}")
        return title
    }

    /**
     * {@inheritDoc}
     */
    @Override
    def getRepositoryDefaultReviewers(String repository, String project = owner) {
        if (!repository?.trim()) {
            return []
        }

        String uri = "https://bitbucket.org/api/2.0/repositories/${project}/${repository}/default-reviewers"
        def response = getPipelineContext().httpRequest(
                authentication: credentialsId,
                httpMode: 'GET',
                url: uri,
                consoleLogResponseBody: log.isDebug()
        )

        return getPipelineContext().readJSON(text: response.content)['values']
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String createPullRequest(
            String repository,
            String sourceBranch,
            String destinationBranch,
            String title,
            String description,
            String project = owner
    ) {

        String uri = "https://bitbucket.org/api/2.0/repositories/${owner}/${repository}/pullrequests"
        String body = JsonOutput.toJson([
                title      : title,
                description: description,
                source     : [
                        branch: [
                                name: sourceBranch
                        ]
                ],
                destination: [
                        branch: [
                                name: destinationBranch
                        ]
                ],
                reviewers  : getRepositoryDefaultReviewers(repository)
        ])

        def response = getPipelineContext().httpRequest(
                httpMode: 'POST',
                url: uri,
                authentication: credentialsId,
                requestBody: body,
                consoleLogResponseBody: log.isDebug()
        )

        Map pr = getPipelineContext().readJSON(text: response.content)
        String link = pr['links']['html']['href']
        log.info("PULL REQUEST WAS CREATED ${link}")
        return link
    }

    /**
     * {@inheritDoc}
     */
    @Override
    List<String> getPullRequestChangedFiles(Map pr) {
        String diff = getPullRequestDiff(pr)
        return getFileListFromDiff(diff)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void updatePullRequestDescriptionSection(Map pr, String section, String body) {
        int pullRequest = pr['id'].toString().toInteger()
        String repository = pr['destination']['repository']['name']

        String uri = "https://bitbucket.org/api/2.0/repositories/${owner}/${repository}/pullrequests/${pullRequest}"

        Map<String, String> descriptionSections = parseDescription(pr['description'].toString())
        descriptionSections.put(section, body)
        def description = descriptionToString(descriptionSections)

        String prUpdate = JsonOutput.toJson([
                title      : pr['title'],
                description: description,
                source     : [
                        branch: [
                                name: pr['source']['branch']['name']
                        ]
                ],
                destination: [
                        branch: [
                                name: pr['destination']['branch']['name']
                        ]
                ],
                reviewers  : pr['reviewers']
        ])

        getPipelineContext().httpRequest(
                authentication: credentialsId,
                contentType: 'APPLICATION_JSON',
                quiet: !log.isDebug(),
                consoleLogResponseBody: log.isDebug(),
                httpMode: 'PUT',
                url: uri,
                requestBody: prUpdate
        )
    }
}

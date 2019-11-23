package com.nextiva.bitbucket

import static com.nextiva.SharedJobsStaticVars.BITBUCKET_SECTION_MARKER

/**
 * Abstract Bitbucket object that could be a self hosted server or hosted cloud instance
 */
abstract class Bitbucket implements Serializable {

    def context

    Bitbucket(context) {
        this.context = context
    }

    /**
     * @return pipeline step context
     */
    Object getPipelineContext() {
        return context
    }

    /**
     * @param description pull request description
     * @return description sections
     */
    static Map<String, String> parseDescription(String description) {
        HashMap<String, String> descriptionSections = new LinkedHashMap<>()

        if (!description?.trim()) {
            return descriptionSections
        }

        List<String> lines = description.split('\n')

        String currentSection = ''

        lines.each { String line ->
            if (line.startsWith(BITBUCKET_SECTION_MARKER)) {
                currentSection = line.replace(BITBUCKET_SECTION_MARKER, '').replace('\n', '')
            } else {
                if (descriptionSections.containsKey(currentSection)) {
                    descriptionSections.put(currentSection, descriptionSections.get(currentSection) + '\n' + line)
                } else {
                    descriptionSections.put(currentSection, line)
                }
            }
        }

        return descriptionSections
    }

    /**
     * @param description description sections
     * @return description string
     */
    static String descriptionToString(Map<String, String> description) {

        String convertedDescription = ''

        description.entrySet().each { Map.Entry<String, String> entry ->
            if (entry.getKey() == '') {
                convertedDescription += entry.getValue()
                if (description.size() > 1) {
                    convertedDescription += '\n'
                }
            } else {
                convertedDescription += BITBUCKET_SECTION_MARKER + entry.getKey() + '\n' + entry.getValue() + '\n'
            }
        }

        // PIPELINE-160 - The maximum number of chars in PR description is 32767
        return convertedDescription.take(32767)
    }

    /**
     * @param url URL from Bitbucket webhook notification
     * @return Parsed JSON response representing pull request
     */
    abstract Map getPullRequestFromUrl(String url)

    /**
     * @param repository slug for repository
     * @param pullRequest the id of the pull request to retrieve
     * @param project the project key or owner of the repository
     * @return Parsed JSON response representing pull request
     */
    abstract Map getPullRequest(String repository, int pullRequest, String project)

    /**
     * @param url URL from Bitbucket webhook notification
     * @return project key
     */
    abstract String getProjectKeyFromUrl(String url)

    /**
     * @param url URL from Bitbucket webhook notification
     * @return repository name
     */
    abstract String getRepositoryNameFromUrl(String url)

    /**
     * @param pr Pull request object {@link Bitbucket#getPullRequestFromUrl(java.lang.String)}
     * @return Pull request author email
     */
    abstract String getPullRequestAuthorEmail(Map pr)

    /**
     * @param pr Pull request object {@link Bitbucket#getPullRequestFromUrl(java.lang.String)}
     * @return Pull request destination branch name
     */
    abstract String getPullRequestDestinationBranch(Map pr)

    /**
     * @param pr Pull request object {@link Bitbucket#getPullRequestFromUrl(java.lang.String)}
     * @return Pull request source branch name
     */
    abstract String getPullRequestSourceBranch(Map pr)

    /**
     * @param pr Pull request object {@link Bitbucket#getPullRequestFromUrl(java.lang.String)}
     * @return Pull request title
     */
    abstract String getPullRequestTitle(Map pr)

    /**
     * @param repository name of repository pr is for
     * @param pr Pull request object {@link Bitbucket#getPullRequestFromUrl(java.lang.String)}
     * @return list of changed files
     */
    abstract List<String> getPullRequestChangedFiles(Map pr)

    /**
     * @param repository name of the repository to fetch default reviewers
     * @param project project key or owner
     * @return list of default repositories
     */
    abstract def getRepositoryDefaultReviewers(String repository, String project)

    /**
     * @param repository slug for the repository
     * @param sourceBranch pull request source branch
     * @param destinationBranch pull request destination branch
     * @param title pull request title
     * @param description pull request description
     * @return link to pull request
     */
    abstract String createPullRequest(
            String repository,
            String sourceBranch,
            String destinationBranch,
            String title,
            String description,
            String project
    )

    /**
     * @param pr Pull request object {@link Bitbucket#getPullRequestFromUrl(java.lang.String)}
     * @param section name of section to update
     * @param body the section content
     */
    abstract void updatePullRequestDescriptionSection(Map pr, String section, String body)
}

@Deprecated
def call(String repositoryUrl, String sourceBranch, String destinationBranch, String title, String description) {
    log.deprecated('Please use bitbucket.createPr() method.')
    return bitbucket.createPr(repositoryUrl, sourceBranch, destinationBranch, title, description)
}
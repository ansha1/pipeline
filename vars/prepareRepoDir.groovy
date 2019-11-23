import static com.nextiva.SharedJobsStaticVars.*


def call(String repo, String branch, String sharedLibraryRepoDir=SHARED_LIBRARY_REPO_DIR) {

    // get exact repo name from the full URL
    // git@bitbucket.org:nextiva/release-management.git -> release-management
    def formattedRepo = common.getRepositoryNameFromUrl(repo)

    // replace trailing slash in branch name to underscore
    // feature/testbranch -> feature_testbranch
    def formattedBranch = branch.replace('/', '_')

    def finalRepoDir = sharedLibraryRepoDir + '/' + formattedRepo + '/' + formattedBranch
    def lockableResource = "lock_" + NODE_NAME.replace(' ', '_') + "_" + finalRepoDir
    log.info("lockableResource: " + lockableResource)

    lock(lockableResource) {
        dir(finalRepoDir) {
            git branch: branch, credentialsId: GIT_CHECKOUT_CREDENTIALS, url: repo
        }
    }

    return finalRepoDir
}

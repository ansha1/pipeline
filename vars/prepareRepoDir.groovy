import static com.nextiva.SharedJobsStaticVars.*


def call(String repo, String branch, String sharedLibraryRepoDir=SHARED_LIBRARY_REPO_DIR) {

    // get exact repo name from the full URL
    // ssh://git@git.nextiva.xyz:7999/rel/release-management.git -> release-management
    def formattedRepo = repo.split('/')[-1].split('\\.')[0]

    // replace trailing slash in branch name to underscore
    // feature/testbranch -> feature_testbranch
    def formattedBranch = branch.replace('/', '_')
    
    def finalRepoDir = sharedLibraryRepoDir + '/' + formattedRepo + '/' + formattedBranch
    def lockableResource = "lock_" + env.NODE_NAME.replace(' ', '_') + "_" + finalRepoDir
    log.info("lockableResource: " + lockableResource)
    
    lock(lockableResource) {
        dir(finalRepoDir) {
            git branch: branch, credentialsId: GIT_CHECKOUT_CREDENTIALS, url: repo
        }
    }

    return finalRepoDir
}

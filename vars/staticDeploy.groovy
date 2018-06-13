import static com.nextiva.SharedJobsStaticVars.*


def call(String componentName, String deployEnvironment, String deployVersion, String nextivaRepo=null) {
    def BASIC_INVENTORY_PATH = 'ansible/role-based_playbooks/inventory/static-deploy'
    def PLAYBOOK_PATH = 'ansible/role-based_playbooks/static-deploy.yml'

    def repo = nextivaRepo.equals(null) ? deployEnvironment : nextivaRepo

    def repoDir = prepareRepoDir(RELEASE_MANAGEMENT_REPO_URL, RELEASE_MANAGEMENT_REPO_BRANCH)
    runAnsiblePlaybook(repoDir, "${BASIC_INVENTORY_PATH}/${env}", PLAYBOOK_PATH,
                       ['version': deployVersion, 'component_name': componentName,
                        'static_assets_files': componentName, 'nextiva_repo': repo])
}

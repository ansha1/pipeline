import static com.nextiva.SharedJobsStaticVars.*


static final BASIC_INVENTORY_PATH = 'ansible/role-based_playbooks/inventory/static-deploy'

def call(String componentName, String deployEnvironment, String deployVersion) {
    def repoDir = prepareRepoDir(RELEASE_MANAGEMENT_REPO_URL, RELEASE_MANAGEMENT_REPO_BRANCH)
    runAnsiblePlaybook(repoDir, "${BASIC_INVENTORY_PATH}/${deployEnvironment}",
                       PLAYBOOK_PATH, ['version': deployVersion, 'component_name': componentName, 'static_assets_files': componentName])
}

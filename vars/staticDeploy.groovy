import static com.nextiva.SharedJobsStaticVars.*


def call(String componentName, String deployEnvironment, String deployVersion, String nextivaRepo=null) {
    static final BASIC_INVENTORY_PATH = 'ansible/role-based_playbooks/inventory/static-deploy'
    static final PLAYBOOK_PATH = 'ansible/role-based_playbooks/static-deploy.yml'

    def repo = nextivaRepo.equals(null) ? deployEnvironment : nextivaRepo

    runAnsiblePlaybook.releaseManagement("${BASIC_INVENTORY_PATH}/${deployEnvironment}", PLAYBOOK_PATH,
                                         ['version': deployVersion, 'component_name': componentName,
                                          'static_assets_files': componentName, 'nextiva_repo': repo])
}

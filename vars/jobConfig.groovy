import com.nextiva.*

def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    ansibleEnvMapDefault = [dev       : "dev",
                            qa        : "qa",
                            production: "production"]

    projectFlow = pipelineParams.projectFlow
    healthCheckMap = pipelineParams.healthCheckMap
    branchPermissionsMap = pipelineParams.branchPermissionsMap
    ansibleEnvMap = pipelineParams.ansibleEnvMap.equals(null) ? ansibleEnvMapDefault : pipelineParams.ansibleEnvMap
    projectLanguages = pipelineParams.projectLanguages
    APP_NAME = pipelineParams.APP_NAME
    BASIC_INVENTORY_PATH = pipelineParams.BASIC_INVENTORY_PATH
    PLAYBOOK_PATH = pipelineParams.PLAYBOOK_PATH
    DEPLOY_APPROVERS = pipelineParams.DEPLOY_APPROVERS
    CHANNEL_TO_NOTIFY = pipelineParams.CHANNEL_TO_NOTIFY


    switch (env.BRANCH_NAME) {
        case 'dev':
            ANSIBLE_ENV = ansibleEnvMap.get('dev')
            healthCheckUrl = healthCheckMap.get('dev')
            branchPermissions = branchPermissionsMap.get('dev')
            DEPLOY_ENVIRONMENT = 'dev'
            DEPLOY_ON_K8S = true
            echo('\nDEPRECATED: Please rename branch "dev" to "develop" to meet git-flow convention.\n')
            break
        case 'develop':
            ANSIBLE_ENV = ansibleEnvMap.get('dev')
            healthCheckUrl = healthCheckMap.get('dev')
            branchPermissions = branchPermissionsMap.get('develop')
            DEPLOY_ENVIRONMENT = 'dev'
            DEPLOY_ON_K8S = true
            break
        case ~/^release\/.+$/:
            ANSIBLE_ENV = ansibleEnvMap.get('qa')
            healthCheckUrl = healthCheckMap.get('qa')
            branchPermissions = branchPermissionsMap.get('qa')
            DEPLOY_ENVIRONMENT = 'production'
            DEPLOY_ON_K8S = false
            break
        case ~/^hotfix\/.+$/:
            ANSIBLE_ENV = 'none'
            healthCheckUrl = ["none"]
            branchPermissions = branchPermissionsMap.get('qa')
            DEPLOY_ENVIRONMENT = 'production'
            DEPLOY_ON_K8S = false
            break
        case 'master':
            ANSIBLE_ENV = ansibleEnvMap.get('production')
            healthCheckUrl = healthCheckMap.get('production')
            branchPermissions = branchPermissionsMap.get('production')
            DEPLOY_ENVIRONMENT = 'production'
            DEPLOY_ON_K8S = false
            break
        default:
            ANSIBLE_ENV = 'none'
            healthCheckUrl = ["none"]
            branchPermissions = branchPermissionsMap.get('dev')
            DEPLOY_ON_K8S = false
            break
    }
    INVENTORY_PATH = BASIC_INVENTORY_PATH + ANSIBLE_ENV
    branchProperties = ['hudson.model.Item.Read:authenticated']
    branchPermissions.each {
        branchProperties.add("hudson.model.Item.Build:${it}")
        branchProperties.add("hudson.model.Item.Cancel:${it}")
    }

    echo('\n\n==============Job config complete ====================\n\n')
    echo("APP_NAME: ${APP_NAME}\n")
    echo("INVENTORY_PATH: ${INVENTORY_PATH}\n")
    echo("PLAYBOOK_PATH: ${PLAYBOOK_PATH}\n")
    echo("DEPLOY_APPROVERS: ${DEPLOY_APPROVERS}\n")
    echo("CHANNEL_TO_NOTIFY: ${CHANNEL_TO_NOTIFY}\n")
    echo("healthCheckUrl:")
    healthCheckUrl.each { print(it) }
    echo('\n======================================================\n')
}

def getUtils() {

    switch (projectFlow.get('language')) {
        case 'java':
            utils = new JavaUtils()
            utils.pathToSrc = projectFlow.get('pathToSrc')

            break
        case 'python':
            utils = new PythonUtils()
            utils.pathToSrc = projectFlow.get('pathToSrc')
            break
        case 'js':
            utils = new JsUtils()
            utils.pathToSrc = projectFlow.get('pathToSrc')
            break
        default:
            error("""Incorrect programming language
                                        please set one of the
                                        supported languages:
                                        java
                                        python
                                        js""")
            break
    }
    return utils
}


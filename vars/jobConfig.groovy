import com.nextiva.*
import static com.nextiva.SharedJobsStaticVars.*


def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    ansibleEnvMapDefault = [dev       : "dev",
                            qa        : "qa",
                            production: "production"]

    projectFlow = pipelineParams.projectFlow
    extraEnvs = pipelineParams.extraEnvs.equals(null) ? [:] : pipelineParams.extraEnvs
    healthCheckMap = pipelineParams.healthCheckMap.equals(null) ? [:] : pipelineParams.healthCheckMap
    branchPermissionsMap = pipelineParams.branchPermissionsMap
    ansibleEnvMap = pipelineParams.ansibleEnvMap.equals(null) ? ansibleEnvMapDefault : pipelineParams.ansibleEnvMap
    jobTimeoutMinutes = pipelineParams.jobTimeoutMinutes.equals(null) ? JOB_TIMEOUT_MINUTES_DEFAULT : pipelineParams.jobTimeoutMinutes
    buildNumToKeepStr = pipelineParams.buildNumToKeepStr.equals(null) ? BUILD_NUM_TO_KEEP_STR : pipelineParams.buildNumToKeepStr
    artifactNumToKeepStr = pipelineParams.artifactNumToKeepStr.equals(null) ? ARTIFACT_NUM_TO_KEEP_STR : pipelineParams.artifactNumToKeepStr
    APP_NAME = pipelineParams.APP_NAME
    nodeLabel = pipelineParams.nodeLabel.equals(null) ? DEFAULT_NODE_LABEL : pipelineParams.nodeLabel
    ansibleRepo = pipelineParams.ansibleRepo.equals(null) ? RELEASE_MANAGEMENT_REPO_URL : pipelineParams.ansibleRepo
    ansibleRepoBranch = pipelineParams.ansibleRepoBranch.equals(null) ? RELEASE_MANAGEMENT_REPO_BRANCH : pipelineParams.ansibleRepoBranch
    BASIC_INVENTORY_PATH = pipelineParams.BASIC_INVENTORY_PATH
    PLAYBOOK_PATH = pipelineParams.PLAYBOOK_PATH
    DEPLOY_APPROVERS = pipelineParams.DEPLOY_APPROVERS
    DEPLOY_ON_K8S = pipelineParams.DEPLOY_ON_K8S.equals(null) ? false : pipelineParams.DEPLOY_ON_K8S
    channelToNotify = pipelineParams.channelToNotify.equals(null) ? null : [(pipelineParams.channelToNotify): "${LIST_OF_DEFAULT_BRANCH_PATTERNS}"]
    slackNotifictionScope = pipelineParams.channelToNotifyPerBranch.equals(null) ? channelToNotify : pipelineParams.channelToNotifyPerBranch

    switch (env.BRANCH_NAME) {
        case 'dev':
            ANSIBLE_ENV = ansibleEnvMap.get('dev')
            healthCheckUrl = healthCheckMap.get('dev')
            branchPermissions = branchPermissionsMap.get('dev')
            DEPLOY_ENVIRONMENT = 'dev'
            echo('\nDEPRECATED: Please rename branch "dev" to "develop" to meet git-flow convention.\n')
            break
        case 'develop':
            ANSIBLE_ENV = ansibleEnvMap.get('dev')
            healthCheckUrl = healthCheckMap.get('dev')
            branchPermissions = branchPermissionsMap.get('develop')
            DEPLOY_ENVIRONMENT = 'dev'
            break
        case ~/^release\/.+$/:
            ANSIBLE_ENV = ansibleEnvMap.get('qa')
            healthCheckUrl = healthCheckMap.get('qa')
            branchPermissions = branchPermissionsMap.get('qa')
            DEPLOY_ENVIRONMENT = 'production'
            break
        case ~/^hotfix\/.+$/:
            ANSIBLE_ENV = 'none'
            healthCheckUrl = []
            branchPermissions = branchPermissionsMap.get('qa')
            DEPLOY_ENVIRONMENT = 'production'
            break
        case 'master':
            ANSIBLE_ENV = ansibleEnvMap.get('production')
            healthCheckUrl = healthCheckMap.get('production')
            branchPermissions = branchPermissionsMap.get('production')
            DEPLOY_ENVIRONMENT = 'production'
            break
        default:
            ANSIBLE_ENV = 'none'
            healthCheckUrl = []
            branchPermissions = branchPermissionsMap.get('dev')
            DEPLOY_ENVIRONMENT = ''
            break
    }
    utils = getUtils(projectFlow.get('language'), projectFlow.get('pathToSrc', '.'))

    INVENTORY_PATH = "${BASIC_INVENTORY_PATH}${ANSIBLE_ENV}"

    branchProperties = ['hudson.model.Item.Read:authenticated']
    branchPermissions.each {
        branchProperties.add("hudson.model.Item.Build:${it}")
        branchProperties.add("hudson.model.Item.Cancel:${it}")
    }

    echo('\n\n==============Job config complete ==================\n\n')
    echo("nodeLabel: ${nodeLabel}\n")
    echo("APP_NAME: ${APP_NAME}\n")
    echo("ansibleRepo: ${ansibleRepo}\n")
    echo("ansibleRepoBranch: ${ansibleRepoBranch}\n")
    echo("INVENTORY_PATH: ${INVENTORY_PATH}\n")
    echo("PLAYBOOK_PATH: ${PLAYBOOK_PATH}\n")
    echo("DEPLOY_APPROVERS: ${DEPLOY_APPROVERS}\n")
    echo("DEPLOY_ENVIRONMENT: ${DEPLOY_ENVIRONMENT}\n")
    echo("DEPLOY_ON_K8S: ${DEPLOY_ON_K8S}\n")
    echo("CHANNEL_TO_NOTIFY: ${slackNotifictionScope}\n")
    echo("healthCheckUrl:")
    healthCheckUrl.each { print(it) }
    echo('\n======================================================\n\n')
}


def getUtils() {
    return utils
}


void setBuildVersion(String userDefinedBuildVersion = null) {
    if (userDefinedBuildVersion) {
        version = userDefinedBuildVersion.trim()
        DEPLOY_ONLY = true
    } else {
        version = utils.getVersion()
        DEPLOY_ONLY = false
    }

    if (env.BRANCH_NAME ==~ /^(dev|develop)$/) {
        BUILD_VERSION = version - "-SNAPSHOT" + "-" + env.BUILD_ID
    } else {
        BUILD_VERSION = version
    }

    echo('===============================')
    echo('BUILD_VERSION: ' + BUILD_VERSION)
    echo('===============================')
    echo('DEPLOY_ONLY: ' + DEPLOY_ONLY)
    echo('===============================')
}


Map getAnsibleExtraVars() {

    switch (projectFlow.get('language')) {
        case 'java':
            ANSIBLE_EXTRA_VARS = ['application_version': version,
                                  'maven_repo'         : version.contains('SNAPSHOT') ? 'snapshots' : 'releases']
            break
        case 'python':
            ANSIBLE_EXTRA_VARS = ['version': BUILD_VERSION]
            break
        case 'js':
            ANSIBLE_EXTRA_VARS = ['version'            : BUILD_VERSION,
                                  'component_name'     : APP_NAME,
                                  'static_assets_files': APP_NAME]
            break
        default:
            error("Incorrect programming language, please set one of the supported languages: java, python, js")
            break
    }

    return ANSIBLE_EXTRA_VARS
}

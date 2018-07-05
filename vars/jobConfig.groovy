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
    CHANNEL_TO_NOTIFY = pipelineParams.CHANNEL_TO_NOTIFY
    defaultSlackNotificationMap = CHANNEL_TO_NOTIFY.equals(null) ? [:] : [(CHANNEL_TO_NOTIFY): LIST_OF_DEFAULT_BRANCH_PATTERNS]
    slackNotifictionScope = pipelineParams.channelToNotifyPerBranch.equals(null) ? defaultSlackNotificationMap : pipelineParams.channelToNotifyPerBranch
    NEWRELIC_APP_ID_MAP = pipelineParams.NEWRELIC_APP_ID_MAP.equals(null) ? [:] : pipelineParams.NEWRELIC_APP_ID_MAP
    jdkVersion = pipelineParams.jdkVersion.equals(null) ?  DEFAULT_JDK_VERSION : pipelineParams.jdkVersion
    mavenVersion = pipelineParams.mavenVersion.equals(null) ? DEFAULT_MAVEN_VERSION : pipelineParams.mavenVersion

    switch (env.BRANCH_NAME) {
        case 'dev':
            ANSIBLE_ENV = ansibleEnvMap.get('dev')
            healthCheckUrl = healthCheckMap.get('dev')
            branchPermissions = branchPermissionsMap.get('dev')
            DEPLOY_ENVIRONMENT = 'dev'
            //log('DEPRECATED: Please rename branch "dev" to "develop" to meet git-flow convention.')
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

    log('')
    log('============== Job config complete ==================')
    log("nodeLabel: ${nodeLabel}")
    log("APP_NAME: ${APP_NAME}")
    log("ansibleRepo: ${ansibleRepo}")
    log("ansibleRepoBranch: ${ansibleRepoBranch}")
    log("INVENTORY_PATH: ${INVENTORY_PATH}")
    log("PLAYBOOK_PATH: ${PLAYBOOK_PATH}")
    log("DEPLOY_APPROVERS: ${DEPLOY_APPROVERS}")
    log("DEPLOY_ENVIRONMENT: ${DEPLOY_ENVIRONMENT}")
    log("DEPLOY_ON_K8S: ${DEPLOY_ON_K8S}")
    log("slackNotifictionScope: ${slackNotifictionScope}")
    log("healthCheckUrl:")
    log("jdkVersion: ${jdkVersion}")
    log("mavenVersion: ${mavenVersion}")
    healthCheckUrl.each { log("  - ${it}") }
    log('=====================================================')
    log('')
}


def getUtils() {
    return utils
}


void setBuildVersion(String userDefinedBuildVersion = null) {
    if (userDefinedBuildVersion) {
        version = userDefinedBuildVersion.trim()
        DEPLOY_ONLY = true
        BUILD_VERSION = version
    } else {
        version = utils.getVersion()
        DEPLOY_ONLY = false

        if (env.BRANCH_NAME ==~ /^(dev|develop)$/) {
            BUILD_VERSION = version - "-SNAPSHOT" + "-" + env.BUILD_ID
        } else {
            BUILD_VERSION = version
        }
    }

    log.info('===============================')
    log.info('BUILD_VERSION: ' + BUILD_VERSION)
    log.info('DEPLOY_ONLY: ' + DEPLOY_ONLY)
    log.info('===============================')
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

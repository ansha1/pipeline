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
    extraEnvs = pipelineParams.extraEnvs ?: [:]
    healthCheckMap = pipelineParams.healthCheckMap ?: [:]
    branchPermissionsMap = pipelineParams.branchPermissionsMap
    ansibleEnvMap = pipelineParams.ansibleEnvMap ?: ansibleEnvMapDefault
    jobTimeoutMinutes = pipelineParams.jobTimeoutMinutes ?: JOB_TIMEOUT_MINUTES_DEFAULT
    buildNumToKeepStr = pipelineParams.buildNumToKeepStr ?: BUILD_NUM_TO_KEEP_STR
    artifactNumToKeepStr = pipelineParams.artifactNumToKeepStr ?: ARTIFACT_NUM_TO_KEEP_STR
    APP_NAME = pipelineParams.APP_NAME
    nodeLabel = pipelineParams.nodeLabel ?: DEFAULT_NODE_LABEL
    ansibleRepo = pipelineParams.ansibleRepo ?: RELEASE_MANAGEMENT_REPO_URL
    ansibleRepoBranch = pipelineParams.ansibleRepoBranch ?: RELEASE_MANAGEMENT_REPO_BRANCH
    BASIC_INVENTORY_PATH = pipelineParams.BASIC_INVENTORY_PATH
    PLAYBOOK_PATH = pipelineParams.PLAYBOOK_PATH
    DEPLOY_APPROVERS = pipelineParams.DEPLOY_APPROVERS
    DEPLOY_ON_K8S = pipelineParams.DEPLOY_ON_K8S ?: false
    CHANNEL_TO_NOTIFY = pipelineParams.CHANNEL_TO_NOTIFY
    defaultSlackNotificationMap = [(CHANNEL_TO_NOTIFY): LIST_OF_DEFAULT_BRANCH_PATTERNS] ?: [:]
    slackNotifictionScope = pipelineParams.channelToNotifyPerBranch ?: defaultSlackNotificationMap
    NEWRELIC_APP_ID_MAP = pipelineParams.NEWRELIC_APP_ID_MAP ?: [:]
    jdkVersion = pipelineParams.jdkVersion ?: DEFAULT_JDK_VERSION
    mavenVersion = pipelineParams.mavenVersion ?: DEFAULT_MAVEN_VERSION
    BLUE_GREEN_DEPLOY = pipelineParams.BLUE_GREEN_DEPLOY ?: BLUE_GREEN_DEPLOY_DEFAULT

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
        case 'implement_PIPELINE-45':
            ANSIBLE_ENV = 'none'
            healthCheckUrl = []
            branchPermissions = branchPermissionsMap.get('implement_PIPELINE-45')
            DEPLOY_ENVIRONMENT = ''
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
        branchProperties.add("hudson.model.Item.Workspace:${it}")
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
    log("branchPermissions: ${branchPermissions}")
    log("DEPLOY_ENVIRONMENT: ${DEPLOY_ENVIRONMENT}")
    log("DEPLOY_ON_K8S: ${DEPLOY_ON_K8S}")
    log("slackNotifictionScope: ${slackNotifictionScope}")
    log("healthCheckUrl:")
    healthCheckUrl.each { log("  - ${it}") }
    log("jdkVersion: ${jdkVersion}")
    log("mavenVersion: ${mavenVersion}")
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


def autoIncrementVersion() {
    try {
        tokens = BUILD_VERSION.tokenize('.')
        major = tokens.get(0)
        minor = tokens.get(1)
        patch = tokens.get(2)
    } catch (e) {
        error('\n\nWrong BUILD_VERSION: ' + version + '\nplease use semantic versioning specification (x.y.z - x: major, y: minor, z: patch)\n\n')
    }

    Integer patch = patch.toInteger() + 1
    patchedBuildVersion = major + "." + minor + "." + patch
    while (utils.verifyPackageInNexus(APP_NAME, patchedBuildVersion, DEPLOY_ENVIRONMENT)) {
        patch += 1
        patchedBuildVersion = major + "." + minor + "." + patch
    }

    return patchedBuildVersion
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

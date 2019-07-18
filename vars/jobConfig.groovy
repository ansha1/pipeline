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

    kubernetesClusterMapDefault = [dev       : "dev.nextiva.io",
                                   qa        : "qa.nextiva.io",
                                   production: "prod.nextiva.io"]

    projectFlow = pipelineParams.projectFlow
    extraEnvs = pipelineParams.extraEnvs ?: [:]
    healthCheckMap = pipelineParams.healthCheckMap ?: [:]
    branchPermissionsMap = pipelineParams.branchPermissionsMap
    ansibleEnvMap = pipelineParams.ansibleEnvMap ?: ansibleEnvMapDefault
    kubernetesClusterMap = pipelineParams.kubernetesClusterMap ?: kubernetesClusterMapDefault
    jobTimeoutMinutes = pipelineParams.jobTimeoutMinutes ?: JOB_TIMEOUT_MINUTES_DEFAULT
    buildNumToKeepStr = pipelineParams.buildNumToKeepStr ?: BUILD_NUM_TO_KEEP_STR
    artifactNumToKeepStr = pipelineParams.artifactNumToKeepStr ?: ARTIFACT_NUM_TO_KEEP_STR
    publishBuildArtifact = getBooleanDefault(pipelineParams.publishBuildArtifact, true)
    publishDockerImage = getBooleanDefault(pipelineParams.publishDockerImage, false)
    APP_NAME = pipelineParams.APP_NAME
    nodeLabel = pipelineParams.nodeLabel ?: DEFAULT_NODE_LABEL
    ansibleRepo = pipelineParams.ansibleRepo ?: RELEASE_MANAGEMENT_REPO_URL
    ansibleRepoBranch = pipelineParams.ansibleRepoBranch ?: RELEASE_MANAGEMENT_REPO_BRANCH
    FULL_INVENTORY_PATH = pipelineParams.FULL_INVENTORY_PATH
    BASIC_INVENTORY_PATH = pipelineParams.BASIC_INVENTORY_PATH
    PLAYBOOK_PATH = pipelineParams.PLAYBOOK_PATH
    DEPLOY_APPROVERS = pipelineParams.DEPLOY_APPROVERS
    DEPLOY_ON_K8S = getBooleanDefault(pipelineParams.DEPLOY_ON_K8S, false)
    ANSIBLE_DEPLOYMENT = getBooleanDefault(pipelineParams.ANSIBLE_DEPLOYMENT, true)
    CHANNEL_TO_NOTIFY = pipelineParams.CHANNEL_TO_NOTIFY
    defaultSlackNotificationMap = [(CHANNEL_TO_NOTIFY): LIST_OF_DEFAULT_BRANCH_PATTERNS] ?: [:]
    slackNotifictionScope = pipelineParams.channelToNotifyPerBranch ?: defaultSlackNotificationMap
    NEWRELIC_APP_ID_MAP = pipelineParams.NEWRELIC_APP_ID_MAP
    NEW_RELIC_APP_ID = pipelineParams.NEW_RELIC_APP_ID
    NEW_RELIC_APP_NAME = pipelineParams.NEW_RELIC_APP_NAME
    jdkVersion = pipelineParams.jdkVersion ?: DEFAULT_JDK_VERSION
    mavenVersion = pipelineParams.mavenVersion ?: DEFAULT_MAVEN_VERSION
    BLUE_GREEN_DEPLOY = getBooleanDefault(pipelineParams.BLUE_GREEN_DEPLOY, false)
    isSecurityScanEnabled = getBooleanDefault(pipelineParams.isSecurityScanEnabled, true)
    isSonarAnalysisEnabled = getBooleanDefault(pipelineParams.isSonarAnalysisEnabled, true)
    veracodeApplicationScope = pipelineParams.veracodeApplicationScope ?: DEFAULT_VERACODE_APPLICATION_SCOPE
    kubernetesDeploymentsList = pipelineParams.kubernetesDeploymentsList ?: [APP_NAME]
    reportDirsList = pipelineParams.reportDirsList ?: []

    // Adding Sales Demo Env Configuration
    deployToSalesDemo = getBooleanDefault(pipelineParams.deployToSalesDemo, false)
    kubernetesClusterSalesDemo = pipelineParams.kubernetesClusterSalesDemo ?: DEFAULT_KUBERNETES_CLUSETER_SALES_DEMO
    inventoryDirectorySalesDemo = pipelineParams.inventoryDirectorySalesDemo ?: DEFAULT_INVENTORY_DIRECTORY_SALES_DEMO
    inventoryPathSalesDemo = "${BASIC_INVENTORY_PATH}${inventoryDirectorySalesDemo}"

    switch (env.BRANCH_NAME) {
        case 'dev':
        case 'develop':
            kubernetesCluster = kubernetesClusterMap.get('dev')
            ANSIBLE_ENV = ansibleEnvMap.get('dev')
            healthCheckUrl = healthCheckMap.get('dev')
            branchPermissions = branchPermissionsMap.get('develop') ?: branchPermissionsMap.get('dev')
            DEPLOY_ENVIRONMENT = 'dev'
            projectFlow['staticAssetsAddress'] = ''
            slackStatusReportChannel = ''
            break
        case ~/^release\/.+$/:
            kubernetesCluster = kubernetesClusterMap.get('qa')
            ANSIBLE_ENV = ansibleEnvMap.get('qa')
            healthCheckUrl = healthCheckMap.get('qa')
            branchPermissions = branchPermissionsMap.get('qa')
            DEPLOY_ENVIRONMENT = 'production'
            projectFlow['staticAssetsAddress'] = PUBLIC_STATIC_ASSETS_ADDRESS
            slackStatusReportChannel = SLACK_STATUS_REPORT_CHANNEL_RC
            break
        case ~/^hotfix\/.+$/:
            kubernetesCluster = 'none'
            ANSIBLE_ENV = ansibleEnvMap.get('qa')
            healthCheckUrl = healthCheckMap.get('qa')
            branchPermissions = branchPermissionsMap.get('qa')
            DEPLOY_ENVIRONMENT = 'production'
            projectFlow['staticAssetsAddress'] = PUBLIC_STATIC_ASSETS_ADDRESS
            slackStatusReportChannel = ''
            break
        case 'master':
            kubernetesCluster = kubernetesClusterMap.get('production')
            ANSIBLE_ENV = ansibleEnvMap.get('production')
            healthCheckUrl = healthCheckMap.get('production')
            branchPermissions = branchPermissionsMap.get('production')
            DEPLOY_ENVIRONMENT = 'production'
            projectFlow['staticAssetsAddress'] = PUBLIC_STATIC_ASSETS_ADDRESS
            slackStatusReportChannel = SLACK_STATUS_REPORT_CHANNEL_PROD
            break
        default:
            kubernetesCluster = 'none'
            ANSIBLE_ENV = 'none'
            healthCheckUrl = []
            branchPermissions = branchPermissionsMap.get('dev') ?: branchPermissionsMap.get('develop')
            DEPLOY_ENVIRONMENT = ''
            projectFlow['staticAssetsAddress'] = ''
            slackStatusReportChannel = ''
            break
    }
    utils = getUtils(projectFlow.get('language'), projectFlow.get('pathToSrc', '.'))

    INVENTORY_PATH = FULL_INVENTORY_PATH ?: "${BASIC_INVENTORY_PATH}${ANSIBLE_ENV}"

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
    log("BRANCH PERMISSIONS: ${branchPermissions}")
    log("DEPLOY_ENVIRONMENT: ${DEPLOY_ENVIRONMENT}")
    log("publishBuildArtifact: ${publishBuildArtifact}")
    log("publishDockerImage: ${publishDockerImage}")
    log("DEPLOY_ON_K8S: ${DEPLOY_ON_K8S}")
    log("ANSIBLE_DEPLOYMENT: ${ANSIBLE_DEPLOYMENT}")
    log("slackNotifictionScope: ${slackNotifictionScope}")
    log("healthCheckUrl:")
    healthCheckUrl.each { log("  - ${it}") }
    log("jdkVersion: ${jdkVersion}")
    log("mavenVersion: ${mavenVersion}")
    log("deployToSalesDemo: ${deployToSalesDemo}")
    log('=====================================================')
    log('')
}

def getUtils() {
    return utils
}


void setBuildVersion(String userDefinedBuildVersion = null) {
    if (userDefinedBuildVersion) {
        semanticVersion = new SemanticVersion(userDefinedBuildVersion.trim())
        version = semanticVersion.toString()
        DEPLOY_ONLY = true
        BUILD_VERSION = version
    } else {
        semanticVersion = new SemanticVersion(utils.getVersion())
        version = semanticVersion.toString()
        DEPLOY_ONLY = false

        if (env.BRANCH_NAME ==~ /^(dev|develop)$/) {
            SemanticVersion buildVersion = semanticVersion.setMeta("${env.BUILD_ID}")
            if (semanticVersion.getPreRelease() ==~ /SNAPSHOT/) {
                buildVersion = buildVersion.setPreRelease("")
            }
            BUILD_VERSION = buildVersion.toString()
        } else {
            BUILD_VERSION = semanticVersion.toString()
        }
    }

    log.info('===============================')
    log.info('BUILD_VERSION: ' + BUILD_VERSION)
    log.info('DEPLOY_ONLY: ' + DEPLOY_ONLY)
    log.info('===============================')
}

void setHotfixDeploy(Boolean hotfixDeploy = false) {
    if (hotfixDeploy) {
        isHotfixDeploy = true
    } else {
        isHotfixDeploy = false
    }

    log.info('===============================')
    log.info('isHotfixDeploy: ' + isHotfixDeploy)
    log.info('===============================')
}

def autoIncrementVersion(SemanticVersion currentVersion) {
    semanticVersion = currentVersion
    version = semanticVersion.toString()
    patchedBuildVersion = currentVersion.toString()

    if (utils.verifyPackageInNexus(APP_NAME, patchedBuildVersion, DEPLOY_ENVIRONMENT)) {
        patchedBuildVersion = autoIncrementVersion(semanticVersion.bump(PatchLevel.PATCH))
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

Boolean getBooleanDefault(def value, Boolean defaultValue) {
    return value == null ? defaultValue : value
}

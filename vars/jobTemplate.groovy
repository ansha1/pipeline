#!groovy
import static com.nextiva.SharedJobsStaticVars.*
import com.nextiva.*


def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    jobConfig {
        extraEnvs = pipelineParams.extraEnvs
        projectFlow = pipelineParams.projectFlow
        healthCheckMap = pipelineParams.healthCheckMap
        branchPermissionsMap = pipelineParams.branchPermissionsMap
        ansibleEnvMap = pipelineParams.ansibleEnvMap
        kubernetesClusterMap = pipelineParams.kubernetesClusterMap
        jobTimeoutMinutes = pipelineParams.jobTimeoutMinutes
        nodeLabel = pipelineParams.NODE_LABEL
        APP_NAME = pipelineParams.APP_NAME
        ansibleRepo = pipelineParams.ANSIBLE_REPO
        ansibleRepoBranch = pipelineParams.ANSIBLE_REPO_BRANCH
        publishBuildArtifact = pipelineParams.publishBuildArtifact
        publishDockerImage = pipelineParams.publishDockerImage
        FULL_INVENTORY_PATH = pipelineParams.FULL_INVENTORY_PATH
        BASIC_INVENTORY_PATH = pipelineParams.BASIC_INVENTORY_PATH
        PLAYBOOK_PATH = pipelineParams.PLAYBOOK_PATH
        DEPLOY_ON_K8S = pipelineParams.DEPLOY_ON_K8S
        ANSIBLE_DEPLOYMENT = pipelineParams.ANSIBLE_DEPLOYMENT
        CHANNEL_TO_NOTIFY = pipelineParams.CHANNEL_TO_NOTIFY
        channelToNotifyPerBranch = pipelineParams.channelToNotifyPerBranch
        buildNumToKeepStr = pipelineParams.buildNumToKeepStr
        artifactNumToKeepStr = pipelineParams.artifactNumToKeepStr
        NEWRELIC_APP_ID_MAP = pipelineParams.NEWRELIC_APP_ID_MAP
        NEW_RELIC_APP_ID = pipelineParams.NEW_RELIC_APP_ID
        NEW_RELIC_APP_NAME = pipelineParams.NEW_RELIC_APP_NAME
        jdkVersion = pipelineParams.JDK_VERSION
        mavenVersion = pipelineParams.MAVEN_VERSION
        BLUE_GREEN_DEPLOY = pipelineParams.BLUE_GREEN_DEPLOY
        isSecurityScanEnabled = pipelineParams.isSecurityScanEnabled
        isSonarAnalysisEnabled = pipelineParams.isSonarAnalysisEnabled
        veracodeApplicationScope = pipelineParams.veracodeApplicationScope
        reportDirsList = pipelineParams.reportDirsList
        // Adding Sales Demo Env Configuration
        deployToSalesDemo = pipelineParams.deployToSalesDemo
        kubernetesClusterSalesDemo = pipelineParams.kubernetesClusterSalesDemo
        inventoryDirectorySalesDemo = pipelineParams.inventoryDirectorySalesDemo
        kubernetesNamespace = pipelineParams.kubernetesNamespace

        // Deprecated
        kubernetesDeploymentsList = pipelineParams.kubernetesDeploymentsList
    }

    def securityPermissions = jobConfig.branchProperties
    def jobTimeoutMinutes = jobConfig.jobTimeoutMinutes
    def buildNumToKeepStr = jobConfig.buildNumToKeepStr
    def artifactNumToKeepStr = jobConfig.artifactNumToKeepStr

    node('master') {

        def appParams = [string(name: 'deploy_version', defaultValue: '', description: 'Set artifact version for skip all steps and deploy only \n' +
                'or leave empty for start full build')]
        if (jobConfig.BLUE_GREEN_DEPLOY && env.BRANCH_NAME == 'master') {
            appParams.add(choice(choices: 'a\nb', description: 'Select A or B when deploying to Production', name: 'stack'))
        } else if (env.BRANCH_NAME ==~ /^hotfix\/.+$/) {
            appParams.add(booleanParam(name: 'hotfix_deploy', description: 'Enable hotfix_deploy to deploy to QA/RC', defaultValue: false))
        }
        if (env.BRANCH_NAME == "master" && jobConfig.deployToSalesDemo) {
            appParams.add(choice(choices: 'prod+sales-demo\nprod\nsales-demo', description: 'Where deploy?', name: 'deployDst'))
        } else if (env.BRANCH_NAME == "master") {
            appParams.add(choice(choices: 'prod', description: 'Where deploy?', name: 'deployDst'))
        }
        appParams.add(booleanParam(name: 'DEBUG', description: 'Enable DEBUG mode with extended output', defaultValue: false))
        properties([
                parameters(appParams)
        ])
    }

//noinspection GroovyAssignabilityCheck
    pipeline {

        agent { label jobConfig.nodeLabel }

        tools {
            jdk jobConfig.jdkVersion
            maven jobConfig.mavenVersion
        }

        options {
            timestamps()
            disableConcurrentBuilds()
            authorizationMatrix(inheritanceStrategy: nonInheriting(), permissions: securityPermissions)
            timeout(time: jobTimeoutMinutes, unit: 'MINUTES')
            buildDiscarder(logRotator(numToKeepStr: buildNumToKeepStr, artifactNumToKeepStr: artifactNumToKeepStr))
            ansiColor('xterm')
        }

        stages {
            stage('Set additional properties') {
                steps {
                    script {
                        utils = jobConfig.getUtils()
                        jobConfig.setBuildVersion(params.deploy_version)
                        jobConfig.setHotfixDeploy(params.hotfix_deploy == null ? false : params.hotfix_deploy)

                        prometheus.sendGauge('build_running', PROMETHEUS_BUILD_RUNNING_METRIC, prometheus.getBuildInfoMap(jobConfig))

                        if (params.stack) {
                            jobConfig.INVENTORY_PATH += "-${params.stack}"
                        }

                        env.APP_NAME = jobConfig.APP_NAME
                        env.INVENTORY_PATH = jobConfig.INVENTORY_PATH
                        env.PLAYBOOK_PATH = jobConfig.PLAYBOOK_PATH
                        env.DEPLOY_ON_K8S = jobConfig.DEPLOY_ON_K8S
                        env.ANSIBLE_DEPLOYMENT = jobConfig.ANSIBLE_DEPLOYMENT
                        env.CHANNEL_TO_NOTIFY = jobConfig.slackNotifictionScope
                        env.DEPLOY_ENVIRONMENT = jobConfig.DEPLOY_ENVIRONMENT
                        env.VERSION = jobConfig.version
                        env.BUILD_VERSION = jobConfig.BUILD_VERSION

                        jobConfig.extraEnvs.each { k, v -> env[k] = v }
                        log.debug('GLOBAL ENVIRONMENT VARIABLES:')
                        log.debug(sh(script: 'printenv', returnStdout: true))
                        log.debug('=============================')

                        if (jobConfig.DEPLOY_ONLY == false && env.BRANCH_NAME ==~ /^((hotfix|release)\/.+)$/) {
                            stage('Release build version verification') {

                                if (utils.verifyPackageInNexus(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.DEPLOY_ENVIRONMENT)) {
                                    try {
                                        timeout(time: 15, unit: 'MINUTES') {
                                            approve("Increase a patch version for ${jobConfig.APP_NAME}",
                                                    "Package *${jobConfig.APP_NAME}* with version *${jobConfig.BUILD_VERSION}* " +
                                                    "already exists in Nexus. \n" +
                                                    "Do you want to increase a patch version and continue the process?",
                                                    "@${common.getCurrentUserSlackId()}",
                                                    "Approve", "Decline", jobConfig.branchPermissions)
                                        }
                                    } catch (e) {
                                        currentBuild.rawBuild.result = Result.ABORTED
                                        throw new hudson.AbortException("Aborted")
                                    }

                                    def patchedBuildVersion = jobConfig.autoIncrementVersion(jobConfig.semanticVersion.bumpPatch())
                                    utils.setVersion(jobConfig.version)

                                    sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                                        sh """
                                            git config --global user.name "Nextiva Jenkins"
                                            git config --global user.email "jenkins@nextiva.com"
                                            git commit -a -m "Auto increment of $jobConfig.BUILD_VERSION - bumped to $patchedBuildVersion"
                                            git push origin HEAD:${BRANCH_NAME}
                                        """
                                    }

                                    jobConfig.setBuildVersion()
                                }
                            }
                        }
                    }
                }
            }
            stage('Unit tests') {
                when {
                    expression { jobConfig.DEPLOY_ONLY ==~ false && !(env.BRANCH_NAME ==~ /^(master)$/) }
                }
                steps {
                    script {
                        sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                            //TODO: remove this condition after all jobs will be migrated from master node
                            if (env.NODE_NAME == "master") {
                                utils.runTests(jobConfig.projectFlow)
                            } else {
                                docker.withRegistry(NEXTIVA_DOCKER_REGISTRY_URL, NEXTIVA_DOCKER_REGISTRY_CREDENTIALS_ID) {
                                    utils.runTests(jobConfig.projectFlow)
                                }
                            }
                        }
                    }
                }
            }
            stage('Sonar analyzing') {
                when {
                    expression {
                        jobConfig.DEPLOY_ONLY ==~ false && env.BRANCH_NAME ==~ /^(develop|dev)$/ && jobConfig.isSonarAnalysisEnabled == true
                    }
                }
                steps {
                    script {
                        utils.runSonarScanner(jobConfig.BUILD_VERSION)
                    }
                }
            }
            stage('Build') {
                when {
                    expression {
                        jobConfig.DEPLOY_ONLY ==~ false && env.BRANCH_NAME ==~ /^(dev|develop|hotfix\/.+|release\/.+)$/
                    }
                }
                stages {
                    stage('Publish build artifacts') {
                        when {
                            expression { jobConfig.publishBuildArtifact == true }
                        }
                        steps {
                            script {
                                utils.buildPublish(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.DEPLOY_ENVIRONMENT, jobConfig.projectFlow)
                            }
                        }
                    }
                    stage('Publish docker image') {
                        when {
                            expression { jobConfig.publishDockerImage == true }
                        }
                        steps {
                            script {
                                buildPublishDockerImage(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.DEPLOY_ENVIRONMENT)
                            }
                        }
                    }
                }
            }
            stage('Security scan') {
                when {
                    expression {
                        jobConfig.DEPLOY_ONLY ==~ false && BRANCH_NAME ==~ /^(release|hotfix)\/.+$/ && jobConfig.isSecurityScanEnabled == true
                    }
                }
                steps {
                    script {
                        def languageVersion = jobConfig.projectFlow.get('languageVersion') ?: 'UNKNOWN'
                        def securityScanJob = "${common.getRepositoryNameFromUrl(env.GIT_URL)}-security-scan"

                        if (jobExists(securityScanJob)) {
                            build job: securityScanJob, parameters: [string(name: 'Branch', value: env.BRANCH_NAME)], wait: false
                        } else {
                            build job: 'securityScan', parameters: [string(name: 'appName', value: jobConfig.APP_NAME),
                                                                    string(name: 'language', value: jobConfig.projectFlow.get('language')),
                                                                    string(name: 'languageVersion', value: languageVersion),
                                                                    string(name: 'pathToSrc', value: jobConfig.projectFlow.get('pathToSrc', '.')),
                                                                    string(name: 'repositoryUrl', value: env.GIT_URL),
                                                                    string(name: 'commitId', value: env.GIT_COMMIT)], wait: false
                        }
                    }
                }
            }
            stage('Deploy') {
                when {
                    expression { env.BRANCH_NAME ==~ /^(dev|develop|master|release\/.+)$/ || jobConfig.isHotfixDeploy }
                }
                stages {
                    stage('Kubernetes deployment') {
                        when {
                            expression {
                                (env.BRANCH_NAME != "master" || params.deployDst ==~ /^(prod\+sales-demo|prod)$/) && jobConfig.DEPLOY_ON_K8S
                            }
                        }
                        steps {
                            script {
                                if (env.BRANCH_NAME ==~ /^(master|release\/.+)$/) {
                                    slack.deployStart(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.ANSIBLE_ENV, jobConfig.slackStatusReportChannel)
                                }
                                log.info("BUILD_VERSION: ${jobConfig.BUILD_VERSION}")
                                log.info("$jobConfig.APP_NAME default $jobConfig.kubernetesCluster $jobConfig.BUILD_VERSION")
                                kubernetes.deploy(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.kubernetesCluster,
                                        [], jobConfig.kubernetesNamespace)
                                newrelic.postDeployment(jobConfig, jobConfig.ANSIBLE_ENV)
                            }
                        }
                    }
                    stage('Sales Demo Kubernetes deployment') {
                        when {
                            expression {
                                env.BRANCH_NAME == 'master' && jobConfig.DEPLOY_ON_K8S && params.deployDst ==~ /^(prod\+sales-demo|sales-demo)$/
                            }
                        }
                        steps {
                            script {

                                try {
                                    log.info("BUILD_VERSION: ${jobConfig.BUILD_VERSION}")
                                    log.info("$jobConfig.APP_NAME default $jobConfig.kubernetesCluster $jobConfig.BUILD_VERSION")
                                    kubernetes.deploy(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.kubernetesClusterSalesDemo,
                                            [], jobConfig.kubernetesNamespace)

                                    newrelic.postDeployment(jobConfig, "demo")
                                } catch (e) {
                                    log.warning("Kubernetes deployment to Sales Demo failed.\n${e}")
                                    currentBuild.result = 'UNSTABLE'
                                }
                            }
                        }
                    }
                    stage('Ansible deployment') {
                        when {
                            expression {
                                (env.BRANCH_NAME != "master" || params.deployDst ==~ /^(prod\+sales-demo|prod)$/) && jobConfig.ANSIBLE_DEPLOYMENT
                            }
                        }
                        steps {
                            script {
                                if (env.BRANCH_NAME ==~ /^(master|release\/.+)$/) {
                                   deployEnv = jobConfig.ANSIBLE_ENV
                                    if (params.stack == "a") {
                                        deployEnv = "STAGING"
                                    }
                                    slack.deployStart(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, deployEnv, jobConfig.slackStatusReportChannel)
                                }

                                sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                                    def repoDir = prepareRepoDir(jobConfig.ansibleRepo, jobConfig.ansibleRepoBranch)
                                    runAnsiblePlaybook(repoDir, jobConfig.INVENTORY_PATH, jobConfig.PLAYBOOK_PATH, jobConfig.getAnsibleExtraVars())
                                }
                                newrelic.postDeployment(jobConfig, jobConfig.ANSIBLE_ENV)
                            }
                        }
                    }
                    stage('Sales Demo Ansible deployment') {
                        when {
                            expression {
                                env.BRANCH_NAME == 'master' && jobConfig.ANSIBLE_DEPLOYMENT && params.deployDst ==~ /^(prod\+sales-demo|sales-demo)$/
                            }
                        }
                        steps {
                            script {
                                
                                try {
                                    slack.deployStart(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, "SALES-DEMO", jobConfig.slackStatusReportChannel)
                                    sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                                        def repoDir = prepareRepoDir(jobConfig.ansibleRepo, jobConfig.ansibleRepoBranch)
                                        runAnsiblePlaybook(repoDir, jobConfig.inventoryPathSalesDemo, jobConfig.PLAYBOOK_PATH, jobConfig.getAnsibleExtraVars())
                                    }

                                    newrelic.postDeployment(jobConfig, "demo")
                                    slack.deployFinish(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, "SALES-DEMO", jobConfig.slackStatusReportChannel)
                                } catch (e) {
                                    log.warning("Ansible deployment to Sales Demo failed.\n${e}")
                                    currentBuild.result = Result.UNSTABLE
                                }
                            }
                        }

                    }
                }
            }
            stage('Healthcheck') {
                when {
                    expression {
                        env.BRANCH_NAME ==~ /^(dev|develop|master|release\/.+)$/ && !(params.deployDst == "sales-demo")
                    }
                }
                steps {
                    script {
                        if (jobConfig.healthCheckUrl.size() > 0) {
                            healthCheck.list(jobConfig.healthCheckUrl)
                        }
                    }
                }
            }
            stage("Post deploy stage") {
                when {
                    expression {
                        jobConfig.projectFlow.get('postDeployCommands') && env.BRANCH_NAME ==~ /^(dev|develop|master|release\/.+)$/ && !(params.deployDst == "sales-demo")
                    }
                }
                steps {
                    script {
                        try {
                            sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                                docker.withRegistry(NEXTIVA_DOCKER_REGISTRY_URL, NEXTIVA_DOCKER_REGISTRY_CREDENTIALS_ID) {
                                    sh jobConfig.projectFlow.get('postDeployCommands')
                                }
                            }
                        } catch (e) {
                            log.warn("there was problem in the post deploy stage $e")
                            currentBuild.result = 'UNSTABLE'
                        } finally {
                            step([$class: 'CucumberReportPublisher']) //PIPELINE-134
                        }
                    }
                }
            }
            stage('QA integration tests') {
                when {
                    expression {
                        env.BRANCH_NAME ==~ /^(dev|develop|master|release\/.+)$/ && !(params.deployDst == "sales-demo")
                    }
                }
                steps {
                    //after successfully deploy on environment start QA CORE TEAM Integration and smoke tests with this application
                    build job: 'test-runner-on-deploy/develop', parameters: [string(name: 'Service', value: jobConfig.APP_NAME),
                                                                             string(name: 'env', value: jobConfig.ANSIBLE_ENV)], wait: false
                }
            }
        }
        post {
            always {
                script {
                    if (jobConfig.reportDirsList.size() > 0) {
                        aws.uploadTestResults(jobConfig.APP_NAME, JOB_NAME, BUILD_ID, jobConfig.reportDirsList)
                    }

                    prometheus.sendGauge('build_running', PROMETHEUS_BUILD_FINISHED_METRIC, prometheus.getBuildInfoMap(jobConfig))
                    prometheus.sendGauge('build_info', System.currentTimeMillis(), prometheus.getBuildInfoMap(jobConfig))

                    if (jobConfig.slackNotifictionScope.size() > 0) {
                        jobConfig.slackNotifictionScope.each { channel, branches ->
                            branches.each {
                                if (env.BRANCH_NAME ==~ it) {
                                    log.info('channel to notify is: ' + channel)
                                    slack.sendBuildStatus(channel)
                                }
                            }
                        }
                    }
                    if (env.BRANCH_NAME ==~ /^(PR.+)$/) {
                        slack.prOwnerPrivateMessage(env.CHANGE_URL)
                    }
                    if (env.BRANCH_NAME ==~ /^(master|release\/.+)$/) {
                        deployEnv = jobConfig.ANSIBLE_ENV
                        if (params.stack == "a") {
                            deployEnv = "STAGING"
                        } else if(params.deployDst == "sales-demo") {
                            deployEnv = "SALES-DEMO"
                        }
                        slack.deployFinish(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, deployEnv, jobConfig.slackStatusReportChannel)
                    }
                }
            }
        }
    }
}

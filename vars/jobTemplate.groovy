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
        DEPLOY_APPROVERS = pipelineParams.DEPLOY_APPROVERS
        CHANNEL_TO_NOTIFY = pipelineParams.CHANNEL_TO_NOTIFY
        channelToNotifyPerBranch = pipelineParams.channelToNotifyPerBranch
        buildNumToKeepStr = pipelineParams.buildNumToKeepStr
        artifactNumToKeepStr = pipelineParams.artifactNumToKeepStr
        NEWRELIC_APP_ID_MAP = pipelineParams.NEWRELIC_APP_ID_MAP
        jdkVersion = pipelineParams.JDK_VERSION
        mavenVersion = pipelineParams.MAVEN_VERSION
        BLUE_GREEN_DEPLOY = pipelineParams.BLUE_GREEN_DEPLOY
        isSecurityScanEnabled = pipelineParams.isSecurityScanEnabled
        isSonarAnalysisEnabled = pipelineParams.isSonarAnalysisEnabled
        veracodeApplicationScope = pipelineParams.veracodeApplicationScope
        kubernetesDeploymentsList = pipelineParams.kubernetesDeploymentsList
        reportDirsList = pipelineParams.reportDirsList
        // Adding Sales Demo Env Configuration
        deployToSalesDemo = pipelineParams.deployToSalesDemo
        kubernetesClusterSalesDemo = pipelineParams.kubernetesClusterSalesDemo
        inventoryDirectorySalesDemo = pipelineParams.inventoryDirectorySalesDemo
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
            appParams.add(string(name: 'deploy_version', defaultValue: '', description: 'Set artifact version for skip all steps and deploy only \n' +
                                    'or leave empty for start full build'))
        } 
        if (env.BRANCH_NAME == "master" && jobConfig.deployToSalesDemo) {
            appParams.add(booleanParam(name: 'salesDemoDeployOnly', description: 'Only Deploy to sales demo', defaultValue: false))
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

                        if (params.salesDemoDeployOnly) {
                            jobConfig.salesDemoDeployOnly = true
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

                                    // Old implementation with non-interactive notification
                                    // It's here to quickly switch to it if jenkins bot doesn't work.
                                    /*
                                    approve.sendToPrivate("Package ${jobConfig.APP_NAME} with version ${jobConfig.BUILD_VERSION} " +
                                            "already exists in Nexus. " +
                                            "Do you want to increase a patch version and continue the process?",
                                            common.getCurrentUserSlackId(), jobConfig.branchPermissions)
                                     */

                                    try {
                                        timeout(time: 15, unit: 'MINUTES') {
                                            bot.getJenkinsApprove("@${common.getCurrentUserSlackId()}", "Approve", "Decline",
                                                    "Increase a patch version for ${jobConfig.APP_NAME}", "${BUILD_URL}input/",
                                                    "Package *${jobConfig.APP_NAME}* with version *${jobConfig.BUILD_VERSION}* " +
                                                            "already exists in Nexus. \n" +
                                                            "Do you want to increase a patch version and continue the process?"
                                                    , "${BUILD_URL}input/", jobConfig.branchPermissions)
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
                        build job: 'securityScan', parameters: [string(name: 'appName', value: jobConfig.APP_NAME),
                                                                string(name: 'language', value: jobConfig.projectFlow.get('language')),
                                                                string(name: 'languageVersion', value: jobConfig.projectFlow.get('languageVersion')),
                                                                string(name: 'pathToSrc', value: jobConfig.projectFlow.get('pathToSrc', '.')),
                                                                string(name: 'repositoryUrl', value: env.GIT_URL),
                                                                string(name: 'commitId', value: env.GIT_COMMIT)], wait: false
                    }
                }
            }
            stage('Deploy') {
                when {
                    expression { env.BRANCH_NAME ==~ /^(dev|develop|master|release\/.+)$/ || jobConfig.isHotfixDeploy }
                }
                parallel {
                    stage('Kubernetes deployment') {
                        when {
                            expression { jobConfig.DEPLOY_ON_K8S == true && jobConfig.salesDemoDeployOnly == false }
                        }
                        steps {
                            script {
                                if (env.BRANCH_NAME ==~ /^(release\/.+)$/) {
                                    slack.deployStart(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.ANSIBLE_ENV, SLACK_STATUS_REPORT_CHANNEL_RC)
                                }
                                log.info("BUILD_VERSION: ${jobConfig.BUILD_VERSION}")
                                log.info("$jobConfig.APP_NAME default $jobConfig.kubernetesCluster $jobConfig.BUILD_VERSION")
                                kubernetes.deploy(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.kubernetesCluster,
                                        jobConfig.kubernetesDeploymentsList)
                            }
                        }
                    }
                    stage('Sales Demo Kubernetes deployment') {
                       when {
                            expression { jobConfig.DEPLOY_ON_K8S == true && jobConfig.deployToSalesDemo == true && env.BRANCH_NAME == 'master' }
                        }
                        steps {
                            script {

                                try {
                                    log.info("BUILD_VERSION: ${jobConfig.BUILD_VERSION}")
                                    log.info("$jobConfig.APP_NAME default $jobConfig.kubernetesCluster $jobConfig.BUILD_VERSION")
                                    kubernetes.deploy(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.kubernetesClusterSalesDemo,
                                        jobConfig.kubernetesDeploymentsList)
                                }
                                catch (e) {
                                    log.warning("Kubernetes deployment to Sales Demo failed.\n${e}")
                                    currentBuild.result = 'UNSTABLE'
                                }
                            }
                        }
                    }
                    stage('Ansible deployment') {
                        when {
                            expression { jobConfig.ANSIBLE_DEPLOYMENT == true && jobConfig.salesDemoDeployOnly == false }
                        }
                        steps {
                            script {
                                if (env.BRANCH_NAME ==~ /^(release\/.+)$/) {
                                    slack.deployStart(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.ANSIBLE_ENV, SLACK_STATUS_REPORT_CHANNEL_RC)
                                }

                                sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                                    def repoDir = prepareRepoDir(jobConfig.ansibleRepo, jobConfig.ansibleRepoBranch)
                                    runAnsiblePlaybook(repoDir, jobConfig.INVENTORY_PATH, jobConfig.PLAYBOOK_PATH, jobConfig.getAnsibleExtraVars())
                                }

                                try {
                                    if (jobConfig.NEWRELIC_APP_ID_MAP.containsKey(jobConfig.ANSIBLE_ENV) && NEWRELIC_API_KEY_MAP.containsKey(jobConfig.ANSIBLE_ENV)) {
                                        newrelic.postBuildVersion(jobConfig.NEWRELIC_APP_ID_MAP[jobConfig.ANSIBLE_ENV], NEWRELIC_API_KEY_MAP[jobConfig.ANSIBLE_ENV],
                                                jobConfig.BUILD_VERSION)
                                    }
                                }
                                catch (e) {
                                    log.warning("An error occurred: Could not log deployment to New Relic. Check integration configuration.\n${e}")
                                }
                            }
                        }
                    }
                    stage('Sales Demo Ansible deployment') {
                       when {
                            expression { jobConfig.ANSIBLE_DEPLOYMENT == true && jobConfig.deployToSalesDemo == true && env.BRANCH_NAME == 'master' }
                        }
                        steps {
                            script {

                                try {
                                    sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                                        def repoDir = prepareRepoDir(jobConfig.ansibleRepo, jobConfig.ansibleRepoBranch)
                                        runAnsiblePlaybook(repoDir, jobConfig.inventoryPathSalesDemo, jobConfig.PLAYBOOK_PATH, jobConfig.getAnsibleExtraVars())
                                    }
                                }
                                catch (e) {
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
                    expression { env.BRANCH_NAME ==~ /^(dev|develop|master|release\/.+)$/ && jobConfig.salesDemoDeployOnly == false }
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
                        jobConfig.projectFlow.get('postDeployCommands') && env.BRANCH_NAME ==~ /^(dev|develop|master|release\/.+)$/ && jobConfig.salesDemoDeployOnly == false
                        }
                }
                steps {
                    script {
                        sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                            docker.withRegistry(NEXTIVA_DOCKER_REGISTRY_URL, NEXTIVA_DOCKER_REGISTRY_CREDENTIALS_ID) {
                                sh jobConfig.projectFlow.get('postDeployCommands')
                            }
                        }
                    }
                }
            }
            stage('QA integration tests') {
                when {
                    expression { env.BRANCH_NAME ==~ /^(dev|develop|master|release\/.+)$/ && jobConfig.salesDemoDeployOnly == false }
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
                    if (env.BRANCH_NAME ==~ /^(release\/.+)$/) {
                        slack.deployFinish(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.ANSIBLE_ENV, SLACK_STATUS_REPORT_CHANNEL_RC)
                    }
                }
            }
        }
    }
}

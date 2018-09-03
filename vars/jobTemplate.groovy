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
        jobTimeoutMinutes = pipelineParams.jobTimeoutMinutes
        nodeLabel = pipelineParams.NODE_LABEL
        APP_NAME = pipelineParams.APP_NAME
        ansibleRepo = pipelineParams.ANSIBLE_REPO
        ansibleRepoBranch = pipelineParams.ANSIBLE_REPO_BRANCH
        BASIC_INVENTORY_PATH = pipelineParams.BASIC_INVENTORY_PATH
        PLAYBOOK_PATH = pipelineParams.PLAYBOOK_PATH
        DEPLOY_ON_K8S = pipelineParams.DEPLOY_ON_K8S
        DEPLOY_APPROVERS = pipelineParams.DEPLOY_APPROVERS
        CHANNEL_TO_NOTIFY = pipelineParams.CHANNEL_TO_NOTIFY
        channelToNotifyPerBranch = pipelineParams.channelToNotifyPerBranch
        buildNumToKeepStr = pipelineParams.buildNumToKeepStr
        artifactNumToKeepStr = pipelineParams.artifactNumToKeepStr
        NEWRELIC_APP_ID_MAP = pipelineParams.NEWRELIC_APP_ID_MAP
        jdkVersion = pipelineParams.JDK_VERSION
        mavenVersion = pipelineParams.MAVEN_VERSION
        BLUE_GREEN_DEPLOY = pipelineParams.BLUE_GREEN_DEPLOY
    }

    def securityPermissions = jobConfig.branchProperties
    def jobTimeoutMinutes = jobConfig.jobTimeoutMinutes
    def buildNumToKeepStr = jobConfig.buildNumToKeepStr
    def artifactNumToKeepStr = jobConfig.artifactNumToKeepStr

    node {
        if (jobConfig.BLUE_GREEN_DEPLOY && env.BRANCH_NAME == 'master') {
            properties([
                    parameters([
                            string(name: 'deploy_version', defaultValue: '', description: 'Set artifact version for skip all steps and deploy only \n' +
                                    'or leave empty for start full build'),
                            choice(choices: 'a\nb', description: 'Select A or B when deploying to Production', name: 'stack')
                    ])
            ])
        } else {
            properties([
                    parameters([
                            string(name: 'deploy_version', defaultValue: '', description: 'Set artifact version for skip all steps and deploy only \n' +
                                    'or leave empty for start full build')
                    ])
            ])
        }
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

                        prometheus.sendGauge('build_running', PROMETHEUS_BUILD_RUNNING_METRIC, prometheus.getBuildInfoMap(jobConfig))

                        if (params.stack) {
                            jobConfig.INVENTORY_PATH += "-${params.stack}"
                        }
                        env.APP_NAME = jobConfig.APP_NAME
                        env.INVENTORY_PATH = jobConfig.INVENTORY_PATH
                        env.PLAYBOOK_PATH = jobConfig.PLAYBOOK_PATH
                        env.DEPLOY_ON_K8S = jobConfig.DEPLOY_ON_K8S
                        env.CHANNEL_TO_NOTIFY = jobConfig.slackNotifictionScope
                        env.DEPLOY_ENVIRONMENT = jobConfig.DEPLOY_ENVIRONMENT
                        env.VERSION = jobConfig.version
                        env.BUILD_VERSION = jobConfig.BUILD_VERSION

                        jobConfig.extraEnvs.each { k, v -> env[k] = v }
                        log.info('GLOBAL ENVIRONMENT VARIABLES:')
                        log.info(sh(script: 'printenv', returnStdout: true))
                        log.info('=============================')

                        if (jobConfig.DEPLOY_ONLY == false && env.BRANCH_NAME ==~ /^((hotfix|release)\/.+)$/) {
                            stage('Release build version verification') {

                                if (utils.verifyPackageInNexus(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.DEPLOY_ENVIRONMENT)) {

                                    approve.sendToPrivate("Package ${jobConfig.APP_NAME} with version ${jobConfig.BUILD_VERSION} " +
                                            "already exists in Nexus. " +
                                            "Do you want to increase a patch version and continue the process?",
                                            common.getCurrentUserSlackId(), jobConfig.branchPermissions)

                                    def patchedBuildVersion = jobConfig.autoIncrementVersion()
                                    utils.setVersion(patchedBuildVersion)

                                    sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                                        sh """
                                            git commit -a -m "Auto increment of ${jobConfig.BUILD_VERSION} - bumped to ${patchedBuildVersion}"
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
                        utils.runTests(jobConfig.projectFlow)

                        // This needs for sending all python projects to the Veracode DEVOPS-1289
                        if (BRANCH_NAME ==~ /^(release\/.+)$/ & jobConfig.projectFlow.language.equals('python')) {
                            stage('Veracode analyzing') {
                                build job: 'VeracodeScan', parameters: [string(name: 'appName', value: jobConfig.APP_NAME),
                                                                        string(name: 'buildVersion', value: jobConfig.BUILD_VERSION),
                                                                        string(name: 'repoUrl', value: GIT_URL),
                                                                        string(name: 'repoBranch', value: BRANCH_NAME)], wait: false
                            }
                        }
                    }
                }
            }
            stage('Sonar analyzing') {
                when {
                    expression { jobConfig.DEPLOY_ONLY ==~ false && env.BRANCH_NAME ==~ /^(dev|develop)$/ }
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
                parallel {
                    stage('Publish build artifacts') {
                        steps {
                            script {
                                utils.buildPublish(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.DEPLOY_ENVIRONMENT, jobConfig.projectFlow)
                            }
                        }
                    }
                    stage('Publish docker image') {
                        when {
                            expression { env.BRANCH_NAME ==~ /^(dev|develop)$/ && jobConfig.DEPLOY_ON_K8S ==~ true }
                        }
                        steps {
                            script {
                                buildPublishDockerImage(jobConfig.APP_NAME, jobConfig.BUILD_VERSION)
                            }
                        }
                    }
                }
            }
            stage('Check approvemets for deploy') {
                when {
                    expression { env.BRANCH_NAME ==~ /^(dev|develop|master|release\/.+)$/ }
                }
                steps {
                    script {
                        if (env.BRANCH_NAME ==~ /^(master|release\/.+)$/) {
//TODO: add approve step, check CR step
//                            approve('Deploy on ' + jobConfig.ANSIBLE_ENV + '?', jobConfig.CHANNEL_TO_NOTIFY, jobConfig.DEPLOY_APPROVERS)
                            isApproved = true //    = approve.isApproved()
                        } else {
                            //always approve for dev branch
                            isApproved = true
                        }
                    }
                }
            }
            stage('Deploy') {
                when {
                    expression { env.BRANCH_NAME ==~ /^(dev|develop|master|release\/.+)$/ }
                }
                parallel {
                    stage('Deploy in kubernetes') {
                        when {
                            expression { env.BRANCH_NAME ==~ /^(dev|develop)$/ && jobConfig.DEPLOY_ON_K8S ==~ true }
                        }
                        steps {
                            script {
                                log.info("BUILD_VERSION: ${jobConfig.BUILD_VERSION}")
                                kubernetes.deploy(jobConfig.APP_NAME, jobConfig.DEPLOY_ENVIRONMENT, 'dev', jobConfig.BUILD_VERSION)
                            }
                        }
                    }
                    stage('Deploy on environment') {
                        when {
                            expression { isApproved ==~ true }
                        }
                        steps {
                            script {
                                def repoDir = prepareRepoDir(jobConfig.ansibleRepo, jobConfig.ansibleRepoBranch)
                                runAnsiblePlaybook(repoDir, jobConfig.INVENTORY_PATH, jobConfig.PLAYBOOK_PATH, jobConfig.getAnsibleExtraVars())

                                try {
                                    if (jobConfig.NEWRELIC_APP_ID_MAP.containsKey(jobConfig.ANSIBLE_ENV) && NEWRELIC_API_KEY_MAP.containsKey(jobConfig.ANSIBLE_ENV)) {
                                        newrelic.postBuildVersion(jobConfig.NEWRELIC_APP_ID_MAP[jobConfig.ANSIBLE_ENV], NEWRELIC_API_KEY_MAP[jobConfig.ANSIBLE_ENV],
                                                jobConfig.BUILD_VERSION)
                                    }
                                }
                                catch (e) {
                                    log.warning("An error occurred: Could not log deployment to New Relic. Check integration configuration.\n${e}")
                                }

                                if (jobConfig.healthCheckUrl.size() > 0) {
                                    stage('Wait until service is up') {
                                        healthCheck.list(jobConfig.healthCheckUrl)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            stage("Post deploy stage") {
                when {
                    expression { jobConfig.projectFlow.get('postDeployCommands') }
                }
                steps {
                    script {
                        sh jobConfig.projectFlow.get('postDeployCommands')
                    }
                }
            }
            stage('QA integration tests') {
                when {
                    expression { env.BRANCH_NAME ==~ /^(dev|develop|master|release\/.+)$/ }
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
                    prometheus.sendGauge('build_running', PROMETHEUS_BUILD_FINISHED_METRIC, prometheus.getBuildInfoMap(jobConfig))
                    prometheus.sendGauge('build_info', PROMETHEUS_DEFAULT_METRIC, prometheus.getBuildInfoMap(jobConfig))

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
                }
            }
        }
    }
}

#!groovy
import com.nextiva.*
import com.nextiva.slack.dto.SlackMessage
import com.nextiva.slack.MessagesFactory
import static com.nextiva.SharedJobsStaticVars.*


def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    repositoryUrl = pipelineParams.repositoryUrl
    projectLanguage = pipelineParams.projectLanguage
    hotfixVersion = pipelineParams.hotfixVersion
    versionPath = pipelineParams.versionPath ?: '.'
    slackChannel = pipelineParams.slackChannel ?: DEFAULT_SLACK_CHANNEL
    APP_NAME = pipelineParams.APP_NAME ?: common.getAppNameFromGitUrl(repositoryUrl)
    jdkVersion = pipelineParams.jdkVersion ?: DEFAULT_JDK_VERSION
    mavenVersion = pipelineParams.mavenVersion ?: DEFAULT_MAVEN_VERSION

//noinspection GroovyAssignabilityCheck
    pipeline {

        agent { label DEFAULT_NODE_LABEL }

        options {
            timestamps()
            ansiColor('xterm')
            disableConcurrentBuilds()
            timeout(time: JOB_TIMEOUT_MINUTES_DEFAULT, unit: 'MINUTES')
            buildDiscarder(logRotator(numToKeepStr: BUILD_NUM_TO_KEEP_STR, artifactNumToKeepStr: ARTIFACT_NUM_TO_KEEP_STR))
        }
        tools {
            jdk jdkVersion
            maven mavenVersion
        }
        stages {
            stage('Checkout repo') {
                steps {
                    cleanWs()

                    git branch: 'master', credentialsId: GIT_CHECKOUT_CREDENTIALS, url: repositoryUrl
                }
            }
            stage('Prepare for starting hotfix') {
                steps {
                    script {
                        utils = getUtils(projectLanguage, versionPath)

                        hotfixBranchList = sh returnStdout: true, script: 'git branch -r | grep "origin/hotfix/" || true'
                        hotfixBranchCount = hotfixBranchList ? hotfixBranchList.split().size() : '0'

                        if (hotfixBranchCount.toInteger() > 0) {
                            log.error('\n\nInterrupting...\nSeems you already have a hotfix branch so we cannot go further with hotfixStart Job!!!\n\n')
                            log.error("hotfix branch count: <<${hotfixBranchCount}>>")
                            log.error("List of hotfix branches:\n${hotfixBranchList}\n")
                            currentBuild.rawBuild.result = Result.ABORTED
                            throw new hudson.AbortException("\n\nhotfix branch(es) already exist, please remove/merge all existing hotfix branches and restart hotfixStart Job!!!\n\n")
                        }
                    }
                }
            }
            stage('Collecting hotfix version') {
                steps {
                    script {
                        log.info("UserDefinedHotfixVersion: ${hotfixVersion}")
                        try {
                            semver = new SemanticVersion(hotfixVersion ?: utils.getVersion())
                        } catch (e) {
                            hotfixVersion = hotfixVersion ?: utils.getVersion()
                            error("""\n\nWrong hotfix version : ${hotfixVersion}
                                    please use git-flow naming convention\n\n""")
                            return
                        }

                        if (!hotfixVersion) {
                            semver = semver.bump(PatchLevel.PATCH)
                        }

                        hotfixVersion = semver.getVersion().toString()

                        if (hotfixVersion ==~ /^(\d+.\d+.\d+)$/) {
                            log.info("Selected hotfix version: ${hotfixVersion}")
                            utils.setVersion(semver.toString())
                        }
                    }
                }
            }
            stage('Create hotfix branch') {
                steps {
                    script {
                        sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                            sh """
                              git checkout -b hotfix/${hotfixVersion}
                              git commit -a -m "Release engineering - bumped to ${hotfixVersion} patch version "
                            """
                        }
                    }
                }
            }
            stage('Push to bitbucket repo') {
                steps {
                    script {
                        sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                            sh """
                              git push --all
                            """
                        }
                    }
                }
            }
        }
        post {
            success {
                script {
                    String user = common.getCurrentUser()
                    SlackMessage slackMessage = new MessagesFactory(this).buildHotfixStartMessage(hotfixVersion, user)
                    slack.sendMessage(slackChannel, slackMessage)
                }
            }
            always {
                script {
                    prometheusLabels = [app_name: APP_NAME, project_language: projectLanguage, version_path: versionPath,
                                        hotfix_version: common.getPropertyValue('hotfixVersion'), channel_to_notify: slackChannel,
                                        application: APP_NAME]

                    prometheus.sendGauge('hotfix_start_info', PROMETHEUS_DEFAULT_METRIC, prometheusLabels)

                    if(currentBuild.currentResult != 'SUCCESS'){
                        slack.sendBuildStatusPrivateMessage(common.getCurrentUserSlackId())
                    }
                }
            }
        }
    }
}

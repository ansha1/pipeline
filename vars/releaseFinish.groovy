#!groovy
import static com.nextiva.SharedJobsStaticVars.*
import com.nextiva.*

def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    repositoryUrl = pipelineParams.repositoryUrl
    developBranch = pipelineParams.developBranch
    projectLanguage = pipelineParams.projectLanguage
    versionPath = pipelineParams.versionPath ?: '.'
    autoPullRequest = pipelineParams.autoPullRequest.equals(null) ? true : pipelineParams.autoPullRequest
    autoMerge = pipelineParams.autoMerge.equals(null) ? true : pipelineParams.autoMerge
    CHANNEL_TO_NOTIFY = pipelineParams.CHANNEL_TO_NOTIFY ?: 'testchannel'
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

            stage('Prepare to finishing release') {
                steps {
                    script {

                        utils = getUtils(projectLanguage, versionPath)

                        releaseBranchCount = sh returnStdout: true, script: 'git branch -r | grep "origin/release/" | wc -l', trim: true
                        releaseBranchCount = releaseBranchCount.trim()
                        log.info("Release branch count: <<${releaseBranchCount}>>")
                        switch (releaseBranchCount) {
                            case '0':
                                log.error('There are no release branches, please run ReleaseStart Job first')
                                currentBuild.rawBuild.result = Result.ABORTED
                                throw new hudson.AbortException("\nThere no release branches, please run ReleaseStart Job first!!!\n")
                                break
                            case '1':
                                releaseBranch = sh returnStdout: true, script: 'git branch -r | grep "origin/release/"'
                                releaseBranch = releaseBranch.trim()
                                log.info('Find release branch ' + releaseBranch + '\ncontinue...\n')
                                break
                            default:
                                log.error('There are more then 1 release branch, please leave one and restart ReleaseFinish Job!!!')
                                currentBuild.rawBuild.result = Result.ABORTED
                                throw new hudson.AbortException("\n\nThere are more then 1 release branches, please leave one and restart ReleaseFinish Job!!!\n\n")
                                break
                        }

                        log.info('Check branch naming for compliance with git-flow')
                        if (releaseBranch ==~ /^(origin\/release\/\d+.\d+(.\d+)?)$/) {
                            log.info('Parse release version')
                            sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                                sh """
                                    git fetch
                                    git checkout ${releaseBranch}
                                """
                            }
                            releaseVersion = utils.getVersion()
                            log.info("Find release version: ${releaseVersion}")
                        } else {
                            error('\n\nWrong release branch name: ' + releaseBranch +
                                    '\nplease use git-flow naming convention\n\n')
                        }

                        if (pipelineParams.developBranch ==~ /^(dev|develop)$/) {
                            log.info('Develop branch looks fine')
                        } else {
                            error('\n\nWrong develop branch name : ' + developBranch +
                                    '\nplease use git-flow naming convention\n\n')
                        }
                    }
                }
            }

            stage('Merge release branch to develop') {
                steps {
                    script {
                        mergeBranches(releaseBranch, developBranch, CHANNEL_TO_NOTIFY, autoPullRequest, autoMerge)
                    }
                }
            }

            stage('Merge release branch to master') {
                steps {
                    script {
                        try {
                            sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                                sh """
                                    git checkout master
                                    git merge --no-ff ${releaseBranch}
                                    git tag -a ${releaseVersion} -m "Merge release branch ${releaseBranch} to master"
                                """
                            }
                        } catch (e) {
                            currentBuild.rawBuild.result = Result.ABORTED
                            throw new hudson.AbortException("\n\nCan`t merge ${releaseBranch} to master \n You need to resolve merge conflicts manually and restart ReleaseFinish Job\n\n")
                        }
                    }
                }
            }

            stage('Push changes in bitbucket') {
                steps {
                    sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                        sh """
                            git push --all
                            git push --tags
                        """
                    }
                }
            }
            stage('Delete release branch') {
                steps {
                    script {
                        releaseBranch = releaseBranch.replace("origin/", "")
                        sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                            sh """
                                git push origin --delete ${releaseBranch}
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
                    def uploadSpec = """[{"title": "Release ${APP_NAME} ${releaseVersion} finished successfully!", "text": "Author: ${user}",
                                        "color": "${SLACK_NOTIFY_COLORS.get(currentBuild.currentResult)}"}]"""
                    slack(CHANNEL_TO_NOTIFY, uploadSpec)
                }
            }
            always {
                script {
                    prometheusLabels = [app_name: APP_NAME, project_language: projectLanguage, develop_branch: developBranch,
                                        version_path: versionPath, auto_pull_request: autoPullRequest, auto_merge: autoMerge,
                                        release_version: common.getPropertyValue('releaseVersion'), channel_to_notify: CHANNEL_TO_NOTIFY,
                                        application: APP_NAME]

                    prometheus.sendGauge('release_finish_info', PROMETHEUS_DEFAULT_METRIC, prometheusLabels)

                    if(currentBuild.currentResult != 'SUCCESS'){
                        slack.sendBuildStatusPrivateMessage(common.getCurrentUserSlackId())
                    }
                }
            }
        }
    }
}

#!groovy
import com.nextiva.*
import static com.nextiva.SharedJobsStaticVars.*

def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    repositoryUrl = pipelineParams.repositoryUrl
    developBranch = pipelineParams.developBranch
    projectLanguage = pipelineParams.projectLanguage
    autoPullRequest = true  // mandatory parameter for hotfix finish
    autoMerge = pipelineParams.autoMerge.equals(null) ? true : pipelineParams.autoMerge
    slackChannel = pipelineParams.slackChannel ?: 'testchannel'
    versionPath = pipelineParams.versionPath ?: '.'
    APP_NAME = pipelineParams.APP_NAME ?: common.getAppNameFromGitUrl(repositoryUrl)

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
            jdk 'Java 8 Install automatically'
            maven 'Maven 3.3.3 Install automatically'
        }
        stages {

            stage('Checkout repo') {
                steps {
                    cleanWs()
                    git branch: 'master', credentialsId: GIT_CHECKOUT_CREDENTIALS, url: repositoryUrl
                }
            }

            stage('Prepare to finishing hotfix') {
                steps {
                    script {

                        utils = getUtils(projectLanguage, versionPath)

                        def hotfixBranches = sh(script: 'git branch -r', returnStdout: true)
                                .tokenize('\n')
                                .collect({ it.trim() })
                                .findAll({ it ==~ /^origin\/hotfix\/\d+.\d+.\d+$/ })

                        log.info("Hotfix branch count: <<${hotfixBranches.size()}>>")
                        switch (hotfixBranches.size()) {
                            case 0:
                                log.error('There are no hotfix branches, please run HotfixStart Job first')
                                currentBuild.rawBuild.result = Result.ABORTED
                                throw new hudson.AbortException("\nThere no hotfix branches, please run HotfixStart Job first!!!\n")
                                break
                            case 1:
                                hotfixBranch = hotfixBranches[0].replace('origin/', '')
                                log.info('Find hotfix branch (' + hotfixBranch + ')\ncontinue...\n')
                                break
                            default:
                                log.error('There are more then 1 hotfix branch, please remove all but one and restart HotfixFinish Job!!!')
                                currentBuild.rawBuild.result = Result.ABORTED
                                throw new hudson.AbortException("\n\nThere are more then 1 hotfix branches, please leave one and restart HotfixFinish Job!!!\n\n")
                                break
                        }

                        log.info('Check branch naming for compliance with git-flow')
                        if (hotfixBranch ==~ /^(hotfix\/\d+.\d+.\d+)$/) {
                            log.info('Parse hotfix version')
                            sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                                sh """
                                    git fetch
                                    git checkout ${hotfixBranch}
                                """
                            }
                            hotfixVersion = utils.getVersion()
                            log.info("Found hotfix version: ${hotfixVersion}")
                        } else {
                            error("""\n\nWrong hotfix branch name: ${hotfixBranch}
                                    please use git-flow naming convention\n\n""")
                        }

                        if (developBranch ==~ /^(dev|develop)$/) {
                            log.info('Develop branch looks fine')
                        } else {
                            error("""\n\nWrong develop branch name : ${developBranch}
                                    please use git-flow naming convention\n\n""")
                        }
                    }
                }
            }

            stage('Merge hotfix branch to master') {
                steps {
                    script {
                        try {
                            sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                                sh """
                                    git fetch
                                    git checkout ${hotfixBranch}
                                    git checkout master
                                    git merge --no-ff ${hotfixBranch}
                                    git tag -a ${hotfixVersion} -m "Merge hotfix branch ${hotfixBranch} to master"
                                """
                            }
                        } catch (e) {
                            currentBuild.rawBuild.result = Result.ABORTED
                            throw new hudson.AbortException("\n\nCan`t merge ${hotfixBranch} to master \n You need to resolve merge conflicts manually and restart HotfixFinish Job\n\n")
                        }
                    }
                }
            }

            stage('Merge hotfix branch to develop and releases') {
                steps {
                    script {
                        def branchesOutput = sh script: 'git branch -r', returnStdout: true
                        def branches = branchesOutput.tokenize('\n')
                                .collect({ it.trim() })
                                .findAll({ it ==~ /^origin\/release\/\d+.\d+(.\d+)?$/ })
                                .collect({ it.trim().replace("origin/", "") })
                        branches.add(developBranch)
                        log.info("Branches to merge to: ${branches}")
                        
                        branches.each { destinationBranch ->
                            try {
                                mergeBranches(hotfixBranch, destinationBranch, slackChannel, autoPullRequest, autoMerge)
                            } catch (e) {
                                log.warn(e)
                                currentBuild.rawBuild.result = Result.UNSTABLE
                            }
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
            stage('Delete hotfix branch') {
                steps {
                    script {
                        sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                            sh """
                                git push origin --delete ${hotfixBranch}
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
                    def uploadSpec = """[{"title": "Hotfix ${APP_NAME} ${hotfixVersion} finished successfully!", "text": "Author: ${user}",
                                        "color": "${SLACK_NOTIFY_COLORS.get(currentBuild.currentResult)}"}]"""
                    slack(slackChannel, uploadSpec)
                }
            }
            always {
                script {
                    prometheus.sendGauge(env.JOB_NAME, 'hotfix_finish_info', 1, prometheus.getJenkinsInfoMap() +
                            [appName: APP_NAME, projectLanguage: projectLanguage, developBranch: developBranch,
                             versionPath: versionPath, autoPullRequest: autoPullRequest, autoMerge: autoMerge,
                             hotfix_version: hotfixVersion, channel_to_notify: slackChannel])

                    if(currentBuild.currentResult != 'SUCCESS'){
                        slack.sendBuildStatusPrivatMessage(common.getCurrentUserSlackId())
                    }
                }
            }
        }
    }
}

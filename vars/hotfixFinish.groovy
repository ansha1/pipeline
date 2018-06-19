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
    autoPullRequest = pipelineParams.autoPullRequest
    slackChannel = pipelineParams.slackChannel
    versionPath = pipelineParams.versionPath.equals(null) ? '.' : pipelineParams.versionPath

    //noinspection GroovyAssignabilityCheck
    pipeline {

        agent any

        options {
            timestamps()
            skipStagesAfterUnstable()
            disableConcurrentBuilds()
            timeout(time: jobTimeoutMinutes, unit: 'MINUTES')
            buildDiscarder(logRotator(numToKeepStr: buildNumToKeepStr, artifactNumToKeepStr: artifactNumToKeepStr))
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

                        echo("Hotfix branch count: <<${hotfixBranches.size()}>>")
                        switch (hotfixBranches.size()) {
                            case 0:
                                echo('There are no hotfix branches, please run HotfixStart Job first')
                                currentBuild.rawBuild.result = Result.ABORTED
                                throw new hudson.AbortException("\nThere no hotfix branches, please run HotfixStart Job first!!!\n")
                                break
                            case 1:
                                hotfixBranch = hotfixBranches[0].replace('origin/', '')
                                echo('Find hotfix branch (' + hotfixBranch + ')\ncontinue...\n')
                                break
                            default:
                                echo('\n\nThere are more then 1 hotfix branch, please remove all but one and restart HotfixFinish Job!!!\n\n')
                                currentBuild.rawBuild.result = Result.ABORTED
                                throw new hudson.AbortException("\n\nThere are more then 1 hotfix branches, please leave one and restart HotfixFinish Job!!!\n\n")
                                break
                        }

                        echo('Check branch naming for compliance with git-flow')
                        if (hotfixBranch ==~ /^(hotfix\/\d+.\d+.\d+)$/) {
                            echo('Parse hotfix version')
                            sh """
                                git fetch
                                git checkout ${hotfixBranch}
                            """
                            hotfixVersion = utils.getVersion()
                            echo("\n\nFind hotfix version: ${hotfixVersion} \n\n")
                        } else {
                            error("""\n\nWrong hotfix branch name: ${hotfixBranch}
                                    please use git-flow naming convention\n\n""")
                        }

                        if (developBranch ==~ /^(dev|develop)$/) {
                            echo('Develop branch looks fine')
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
                            sh """
                                git fetch
                                git checkout ${hotfixBranch}
                                git checkout master
                                git merge --no-ff ${hotfixBranch}
                                git tag -a ${hotfixVersion} -m "Merge hotfix branch ${hotfixBranch} to master"
                            """
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
                                .findAll({ it ==~ /^origin\/release\/\d+.\d+.\d+$/ })
                                .collect({ it.trim().replace("origin/", "") })
                        branches.add(developBranch)
                        echo("Branches to merge to: ${branches}")
                        branches.each { branchToMerge ->
                            mergeBranch(repositoryUrl, hotfixBranch, branchToMerge, slackChannel, autoPullRequest)
                        }
                    }
                }
            }

            stage('Push changes in bitbucket') {
                steps {
                    sh """
                        git push --all
                        git push --tags
                    """
                }
            }
            stage('Delete hotfix branch') {
                steps {
                    script {
                        sh """
                            git push origin --delete ${hotfixBranch}
                        """
                    }
                }
            }
        }
    }
}

def mergeBranch(repositoryUrl, source, destination, channelToNotify, autoPullRequest) {
    try {
        sh """
            git checkout ${destination}
            git checkout ${source}
            git merge --no-ff ${destination}
        """
        slackSend(color: '#00FF00', channel: channelToNotify, message: "merged ${source} into ${destination}")
    } catch (e) {
        print e
        if (autoPullRequest) {
            def newBranch = "${source}-to-${destination}"
            sh """
                git reset --hard
                git checkout -b ${newBranch}
                git push origin ${newBranch}
            """
            def prLink = createPr(repositoryUrl, newBranch, destination, "${source}->${destination}", 'DO NOT SELECT SQUASH OPTION WHEN MERGING THIS PR(if its enabled for the repository), otherwise there will be conflicts when merging release to master and releaseFinish job fill fail')
            slackSend(color: '#6600cc',
                    channel: channelToNotify,
                    message: "Failed to automatically merge ${source} into ${destination}. Resolve conflicts and merge pull request (${prLink})")
        }
    }
}
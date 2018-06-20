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
    versionPath = pipelineParams.versionPath.equals(null) ? '.' : pipelineParams.versionPath
    autoPullRequest = pipelineParams.autoPullRequest.equals(null) ? false : pipelineParams.autoPullRequest
    CHANNEL_TO_NOTIFY = pipelineParams.CHANNEL_TO_NOTIFY

//noinspection GroovyAssignabilityCheck
    pipeline {

        agent any

        options {
            timestamps()
            skipStagesAfterUnstable()
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

            stage('Prepare to finishing release') {
                steps {
                    script {

                        utils = getUtils(projectLanguage, versionPath)

                        releaseBranchCount = sh returnStdout: true, script: 'git branch -r | grep "^  origin/release/" | wc -l', trim: true
                        releaseBranchCount = releaseBranchCount.trim()
                        log.info("Release branch count: <<${releaseBranchCount}>>")
                        switch (releaseBranchCount) {
                            case '0':
                                log.error('There are no release branches, please run ReleaseStart Job first')
                                currentBuild.rawBuild.result = Result.ABORTED
                                throw new hudson.AbortException("\nThere no release branches, please run ReleaseStart Job first!!!\n")
                                break
                            case '1':
                                releaseBranch = sh returnStdout: true, script: 'git branch -r | grep "^  origin/release/"'
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
                        if (releaseBranch ==~ /^(origin\/release\/\d+.\d+.\d+)$/) {
                            log.info('Parse release version')
                            sh """
                                git fetch
                                git checkout ${releaseBranch}
                            """
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
                        try {
                            sh """
                            git checkout ${developBranch}
                            git merge --no-ff ${releaseBranch}
                        """
                        } catch (e) {
                            if (autoPullRequest) {
                                log.info("AUTO CREATING PULL REQUEST IS ENABLED  autoPullRequest: ${autoPullRequest}")
                                stage("Create temporary branch")
                                def tmpBranch = "resolve-conflicts-from-${releaseBranch.replaceAll("origin/","")}-to-${developBranch.replaceAll("origin/","")}"
                                sh """
                                    git reset --merge           
                                    git checkout ${releaseBranch}
                                    git checkout -b ${tmpBranch} ${releaseBranch}
                                    git push origin ${tmpBranch}
                                """
                                stage("Create pull request from ${releaseBranch} to ${developBranch}")
                                def title = "DO NOT SQUASH THIS PR. Resolve merge conflicts for finishing ${env.JOB_NAME}#${env.BUILD_ID} "
                                def description = "DO NOT SELECT SQUASH OPTION WHEN MERGING THIS PR (if its enabled for the repository). Auto created pull request from ${env.JOB_NAME} #${env.BUILD_ID}"
                                pullRequestLink = createPr(repositoryUrl, tmpBranch, developBranch, title, description)

                                def uploadSpec = """[{
                                                        "title": "${title}",
                                                        "text": "${description}",
                                                        "color": "#73797a",
                                                        "attachment_type": "default",
                                                        "actions": [
                                                            {
                                                                "text": "Link on pull request",
                                                                "type": "button",
                                                                "url": "${pullRequestLink}"
                                                            }
                                                        ]
                                                    }]"""
                                slackSend(channel: CHANNEL_TO_NOTIFY, attachments: uploadSpec, tokenCredentialId: "slackToken")
                                currentBuild.result = 'UNSTABLE'
                                error("\n\nCan`t merge ${releaseBranch} to ${developBranch} \n You need to resolve merge conflicts in branch: ${tmpBranch} pull request: ${pullRequestLink} and restart ReleaseFinish Job\n\n")
                            } else {
                                log.info("AUTO CREATING PULL REQUEST IS DISABLED  autoPullRequest: ${autoPullRequest}")
                                currentBuild.rawBuild.result = Result.ABORTED
                                throw new hudson.AbortException("\n\nCan`t merge ${releaseBranch} to ${developBranch} \n You need to resolve merge conflicts manually and restart ReleaseFinish Job\n\n")
                            }
                        }
                    }
                }
            }

            stage('Merge release branch to master') {
                steps {
                    script {
                        try {
                            sh """
                                git fetch
                                git checkout ${releaseBranch}
                                git checkout master
                                git merge --no-ff ${releaseBranch}
                                git tag -a ${releaseVersion} -m "Merge release branch ${releaseBranch} to master"
                            """
                        } catch (e) {
                            currentBuild.rawBuild.result = Result.ABORTED
                            throw new hudson.AbortException("\n\nCan`t merge ${releaseBranch} to master \n You need to resolve merge conflicts manually and restart ReleaseFinish Job\n\n")
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
            stage('Delete release branch') {
                steps {
                    script {
                        releaseBranch = releaseBranch.replace("origin/", "")
                        sh """
                            git push origin --delete ${releaseBranch}
                        """
                    }
                }
            }
        }
    }
}

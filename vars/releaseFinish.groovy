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

                        switch (projectLanguage) {
                            case 'java':
                                utils = new JavaUtils()
                                break
                            case 'python':
                                utils = new PythonUtils()
                                break
                            case 'js':
                                utils = new JsUtils()
                                break
                            default:
                                error("Incorrent programming language\n" +
                                        "please set one of the\n" +
                                        "supported languages:\n" +
                                        "java\n" +
                                        "python\n" +
                                        "js\n")
                                break
                        }

                        releaseBranchCount = sh returnStdout: true, script: 'git branch -r | grep "^  origin/release/" | wc -l', trim: true
                        releaseBranchCount = releaseBranchCount.trim()
                        echo("Release branch count: <<${releaseBranchCount}>>")
                        switch (releaseBranchCount) {
                            case '0':
                                echo('There are no release branches, please run ReleaseStart Job first')
                                currentBuild.rawBuild.result = Result.ABORTED
                                throw new hudson.AbortException("\nThere no release branches, please run ReleaseStart Job first!!!\n")
                                break
                            case '1':
                                releaseBranch = sh returnStdout: true, script: 'git branch -r | grep "^  origin/release/"'
                                releaseBranch = releaseBranch.trim()
                                echo('Find release branch ' + releaseBranch + '\ncontinue...\n')
                                break
                            default:
                                echo('\n\nThere are more then 1 release branch, please leave one and restart ReleaseFinish Job!!!\n\n')
                                currentBuild.rawBuild.result = Result.ABORTED
                                throw new hudson.AbortException("\n\nThere are more then 1 release branches, please leave one and restart ReleaseFinish Job!!!\n\n")
                                break
                        }

                        echo('Check branch naming for compliance with git-flow')
                        if (releaseBranch ==~ /^(origin\/release\/\d+.\d+.\d+)$/) {
                            echo('Parse release version')
                            sh """
                                git fetch
                                git checkout ${releaseBranch}
                            """
                            releaseVersion = utils.getVersion(versionPath)
                            echo("\n\nFind release version: ${releaseVersion} \n\n")
                        } else {
                            error('\n\nWrong release branch name: ' + releaseBranch +
                                    '\nplease use git-flow naming convention\n\n')
                        }

                        if (pipelineParams.developBranch ==~ /^(dev|develop)$/) {
                            echo('Develop branch looks fine')
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
                                print("\n\n AUTO CREATING PULL REQUEST IS ENABLED  autoPullRequest: ${autoPullRequest}")
                                stage("Create temporary branch")
                                def tmpBranch = "resolve_conflicts_from_${releaseBranch}"
                                sh """
                                    git reset --merge           
                                    git checkout ${releaseBranch}
                                    git checkout -b ${tmpBranch} ${releaseBranch}
                                    git push origin ${tmpBranch}
                                """
                                stage("Create pull request from ${releaseBranch} to ${developBranch}")
                                def title = "Resolve megre conflicts for finishing ${env.JOB_NAME}#${env.BUILD_ID} "
                                def description = "Auto created pull request from ${env.JOB_NAME} #${env.BUILD_ID}"
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
                                slackSend(channel: notifyChannel, attachments: uploadSpec, tokenCredentialId: "slackToken")
                                currentBuild.result = 'UNSTABLE'
                                error("\n\nCan`t merge ${releaseBranch} to ${developBranch} \n You need to resolve merge conflicts in branch: ${tmpBranch} pull request: ${pullRequestLink} and restart ReleaseFinish Job\n\n")
                            } else {
                                print("\n\n AUTO CREATING PULL REQUEST IS DISABLED  autoPullRequest: ${autoPullRequest}")
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

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
    userDefinedReleaseVersion = pipelineParams.userDefinedReleaseVersion
    versionPath = pipelineParams.versionPath ?: '.'
    CHANNEL_TO_NOTIFY = pipelineParams.CHANNEL_TO_NOTIFY ?: 'testchannel'
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
                    git branch: developBranch, credentialsId: GIT_CHECKOUT_CREDENTIALS, url: repositoryUrl
                }
            }

            stage('Prepare for starting release') {
                steps {
                    script {
                        utils = getUtils(projectLanguage, versionPath)

                        releaseBranchList = sh returnStdout: true, script: 'git branch -r | grep "origin/release/" || true'
                        releaseBranchCount = releaseBranchList ? releaseBranchList.split().size() : '0'

                        if (releaseBranchCount.toInteger() > 0) {
                            log.error('\n\nInterrupting...\nSeems you already have a release branch so we cannot go further with ReleaseStart Job!!!\n\n')
                            log.error("Release branch count: <<${releaseBranchCount}>>")
                            log.error("List of release branches:\n${releaseBranchList}\n")
                            currentBuild.rawBuild.result = Result.ABORTED
                            throw new hudson.AbortException("\n\nRelease branch(es) already exist, please remove/merge all existing release branches and restart ReleaseStart Job!!!\n\n")
                        }
                    }
                }
            }

            stage('Collecting release version') {
                steps {
                    script {

                        releaseVersion = userDefinedReleaseVersion ?: utils.getVersion()
                        releaseVersion = releaseVersion.replace("-SNAPSHOT", "")

                        if (releaseVersion ==~ /^(\d+.\d+(.\d+)?)$/) {
                            log.info("Selected release version: ${releaseVersion}")

                            tokens = releaseVersion.tokenize('.')
                            major = tokens.get(0)
                            minor = tokens.get(1)

                            if (tokens.size() == 2) {
                                releaseBranch = releaseVersion
                                releaseVersion = major + "." + minor + "." + "0"
                                log.warning("The version we are going to use for deployment is: ${releaseVersion}")
                            } else if (userDefinedReleaseVersion) {
                                log.info("UserDefinedReleaseVersion: ${userDefinedReleaseVersion}")
                                releaseBranch = releaseVersion
                            } else {
                                log.info("Getting releaseVersion from build properties file")
                                releaseBranch = major + '.' + minor
                            }
                        } else {
                            error('\n\nWrong release version : ' + releaseVersion +
                                    '\nplease use git-flow naming convention\n\n')
                        }
                    }
                }
            }

            stage('Create release branch') {
                steps {
                    script {
                        utils.setVersion(releaseVersion)
                        sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                            sh """
                                git commit --allow-empty -a -m "Release engineering - bumped to ${releaseBranch} release candidate version "
                                git branch release/${releaseBranch}
                            """
                        }
                    }
                }
            }

            stage('Next development version bump') {
                steps {
                    script {
                        def developmentVersionPrefix = major + "." + (minor.toInteger() + 1) + "." + "0"

                        switch (projectLanguage) {
                            case 'java':
                                developmentVersion = developmentVersionPrefix + "-SNAPSHOT"
                                break
                            default:
                                developmentVersion = developmentVersionPrefix
                        }

                        log.info('developmentVersion: ' + developmentVersion)
                        utils.setVersion(developmentVersion)

                        sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                            sh """
                              git commit -a -m "Release engineering - bumped to ${developmentVersion} next development version"
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
                    def uploadSpec = """[{"title": "Release ${APP_NAME} ${releaseVersion} started successfully!", "text": "Author: ${user}",
                                        "color": "${SLACK_NOTIFY_COLORS.get(currentBuild.currentResult)}"}]"""
                    slack(CHANNEL_TO_NOTIFY, uploadSpec)
                }
            }
            always {
                script {
                    if(currentBuild.currentResult != 'SUCCESS'){
                        slack.sendBuildStatusPrivatMessage(common.getCurrentUserSlackId())
                    }
                }
            }
        }
    }
}
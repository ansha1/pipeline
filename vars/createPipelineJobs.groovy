#!groovy
import static com.nextiva.SharedJobsStaticVars.*

def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    repositoryUrl = pipelineParams.repositoryUrl
    developBranch = pipelineParams.developBranch
    projectLanguage = pipelineParams.projectLanguage
    versionPath = pipelineParams.versionPath ?: '.'
    slackChannel = pipelineParams.slackChannel ?: 'testchannel'
    applicationName = pipelineParams.applicationName ?: common.getAppNameFromGitUrl(repositoryUrl.toLowerCase())
                                                              .replace('-', ' ')
                                                              .replace('_', ' ')
                                                              .split(' ')
                                                              .collect({ it.capitalize() })
                                                              .join(' ')
    applicationSlug = pipelineParams.applicationSlug ?: applicationName.toLowerCase()
                                                              .replace(' ', '-')
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

            stage('Checkout') {
                steps {
                    cleanWs()
                    git branch: 'master', credentialsId: GIT_CHECKOUT_CREDENTIALS, url: 'ssh://git@git.nextiva.xyz:7999/ops/jenkins-jobdsl.git'
                }
            }

            stage('Prepare Config File') {
                steps {
                    script {
                        sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                            sh """cat <<CONFIG > projects/${applicationSlug}.yml
name: ${applicationName}
slug: ${applicationSlug}
language: ${projectLanguage}
branch: ${developBranch}
repository: ${repositoryUrl}
path: ${versionPath}
channel: ${slackChannel}
unmanaged: false
jdk: ${jdkVersion}
maven: ${mavenVersion}
CONFIG"""
                        }
                    }
                }
            }

            stage('Commit File') {
                steps {
                    script {
                        sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                            sh """
                                git add projects/${applicationSlug}.yml
                                git commit --allow-empty -m "ci(${applicationSlug}): provide ${applicationSlug} job config"
                            """
                        }
                    }
                }
            }

            stage('Push to BitBucket') {
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
                    def uploadSpec = """[{"title": "Jenkins job configuration added for ${applicationName}!", "text": "Author: ${user}",
                                        "color": "${SLACK_NOTIFY_COLORS.get(currentBuild.currentResult)}"}]"""
                    slack(slackChannel, uploadSpec)
                }
            }
            always {
                script {
                    if(currentBuild.currentResult != 'SUCCESS'){
                        slack.sendBuildStatusPrivateMessage(common.getCurrentUserSlackId())
                    }
                }
            }
        }
    }
}
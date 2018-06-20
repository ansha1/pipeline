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
    userDefinedReleaseVersion = pipelineParams.userDefinedReleaseVersion
    versionPath = pipelineParams.versionPath.equals(null) ? '.' : pipelineParams.versionPath

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

                    git branch: developBranch, credentialsId: GIT_CHECKOUT_CREDENTIALS, url: repositoryUrl

                }
            }

            stage('Prepare for starting release') {
                steps {
                    script {
                        utils = getUtils(projectLanguage, versionPath)

                        releaseBranchList = sh returnStdout: true, script: 'git branch -r | grep "origin/release/" || true'
                        releaseBranchCount = releaseBranchList.equals(null) ? '0' : releaseBranchList.split().size()

                        if (releaseBranchCount.toInteger() > 0) {
                            log.error('\n\nInterrupting...\nSeems you already have a release branch so we cannnot go further with ReleaseStart Job!!!\n\n')
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
                        log.info("UserDefinedReleaseVersion: ${userDefinedReleaseVersion}")

                        if (userDefinedReleaseVersion.equals('')) {
                            releaseVersionNumList = utils.getVersion().tokenize('.')
                            releaseVersion = releaseVersionNumList.get(0) + '.' + releaseVersionNumList.get(1)
                        } else {
                            releaseVersion = userDefinedReleaseVersion
                            if (releaseVersion ==~ /^(\d+.\d+(.\d+)?)$/) {
                                log.info("Selected release version: ${releaseVersion}")
                                releaseVersion = releaseVersion.replace("-SNAPSHOT", "")
                            } else {
                                error('\n\nWrong release version : ' + releaseVersion +
                                    '\nplease use git-flow naming convention\n\n')
                            }
                        }                        
                    }
                }
            }

            stage('Create release branch') {
                steps {
                    script {
                        utils.setVersion(releaseVersion)
                        sh """
                            git commit --allow-empty -m "Release engineering - bumped to ${releaseVersion} release candidate version "
                        """
                        sh "git branch release/${releaseVersion}"
                    }
                }
            }

            stage('Next development version bump') {
                steps {
                    script {
                        def tokens = releaseVersion.tokenize('.')
                        def major = tokens.get(0)
                        def minor = tokens.get(1)
            
                        if (tokens.size() == 3) {
                            suffix = ".0"
                        } else {
                            suffix = ""
                        }

                        def developmentVersionPrefix = (major + "." + (minor.toInteger() + 1) + suffix).trim()
                        log.info('developmentVersionPrefix: ' + developmentVersionPrefix)

                        switch (projectLanguage) {
                            case 'java':
                                developmentVersion = developmentVersionPrefix + "-SNAPSHOT"
                                break
                            default:
                                developmentVersion = developmentVersionPrefix
                        }

                        utils.setVersion(developmentVersion)

                        sh """
                          git commit -a -m "Release engineering - bumped to ${developmentVersion} next development version"
                        """
                    }
                }
            }

            stage('Push to bitbucket repo') {
                steps {
                    script {
                        sh """
                          git push --all
                        """
                    }
                }
            }
        }
    }
}
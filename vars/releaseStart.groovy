#!groovy
package com.nextiva
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
    versionPath = pipelineParams.versionPath.equals(null) ? '.' : pipelineParams.versionPath

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

                    git branch: developBranch, credentialsId: GIT_CHECKOUT_CREDENTIALS, url: repositoryUrl

                }
            }
            stage('Prepare for starting release') {
                steps {
                    script {
                        utils = getUtils(projectLanguage, versionPath)
                    }
                }
            }

            stage('Collecting release version') {
                steps {
                    script {
                        echo "\nUserDefinedReleaseVersion: ${userDefinedReleaseVersion}\n"
                        releaseVersion = userDefinedReleaseVersion.equals('') ? utils.getVersion() : userDefinedReleaseVersion
                        releaseVersion = releaseVersion.replace("-SNAPSHOT", "")

                        if (releaseVersion ==~ /^(\d+.\d+.\d+)$/) {
                            echo("\n\nSelected release version: ${releaseVersion}")
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
                        if (projectLanguage.equals('java')) {
                            utils.setVersion(releaseVersion)
                            //set release version in dev branch for prevent merge conflicts
                            sh """
                              git commit -a -m "Release engineering - bumped to ${releaseVersion} release candidate version "
                            """
                        }
                        sh """
                          git branch release/${releaseVersion}
                        """
                    }
                }
            }
            stage('Next development version bump') {
                steps {
                    script {
                        def tokens = releaseVersion.tokenize('.')
                        def major = tokens.get(0)
                        def minor = tokens.get(1)
                        def patch = tokens.get(2)

                        switch (projectLanguage) {
                            case 'java':
                                developmentVersion = major + "." + (minor.toInteger() + 1) + "." + "0" + "-SNAPSHOT"
                                break
                            default:
                                developmentVersion = major + "." + (minor.toInteger() + 1) + "." + "0"
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
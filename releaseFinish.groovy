#!groovy
@Library('pipelines') _
import static com.nextiva.SharedJobsStaticVars.*

//noinspection GroovyAssignabilityCheck
pipeline {

    agent any

    options {
        timestamps()
        skipStagesAfterUnstable()
    }

    parameters {
        string(name: 'repositoryUrl', description: 'repository url where you want to finish release', trim: true)
        string(name: 'releaseBranch', description: 'enter release branch ex. release/1.2.3', trim: true)
        string(name: 'developBranch', defaultValue: 'dev', description: 'enter develop branch or leave empty to use default \'develop\' ', trim: true)
    }

    stages {
        stage('Prepare to finishing release') {
            steps {
                script {
                    echo('Check branch naming for compliance with git-flow')
                    if (params.releaseBranch ==~ /^(release\/\d+.\d+.\d+)$/) {
                        echo('Parse release version')
                        releaseVersion = params.releaseBranch.replace("release/", "")
                    } else {
                        error('Wrong release branchName \n' +
                                'please use git-flow naming convention')
                    }

                    if (params.developBranch ==~ /^(dev|develop)$/) {
                        echo('Develop branch looks fine')
                    } else {
                        error('Wrong release branchName \n' +
                                'please use git-flow naming convention')
                    }
                }
            }
        }
        stage('Checkout repo') {
            steps {
                cleanWs()

                git branch: 'master', credentialsId: GIT_CHECKOUT_CREDENTIALS, url: params.repositoryUrl

            }
        }
        stage('Merge release branch to master') {
            steps {
                sh """
                    git fetch
                    git checkout ${params.releaseBranch}
                    git checkout master
                    git merge --no-ff ${params.releaseBranch}
                    git tag -a ${releaseVersion} -m "Merge release branch ${params.releaseBranch} to master"
                """

            }
        }
        stage('Merge release branch to develop') {
            steps {
                sh """
                    git checkout ${params.developBranch}
                    git merge --no-ff ${params.releaseBranch}
                """
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
                sh """
                    git branch -d ${params.releaseBranch}
                """
            }
        }
    }
}


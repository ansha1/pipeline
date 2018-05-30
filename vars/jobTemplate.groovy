#!groovy
import static com.nextiva.SharedJobsStaticVars.*
import com.nextiva.*

def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    jobConfig {
        extraEnvs = pipelineParams.extraEnvs
        projectFlow = pipelineParams.projectFlow
        healthCheckMap = pipelineParams.healthCheckMap
        branchPermissionsMap = pipelineParams.branchPermissionsMap
        ansibleEnvMap = pipelineParams.ansibleEnvMap
        APP_NAME = pipelineParams.APP_NAME
        BASIC_INVENTORY_PATH = pipelineParams.BASIC_INVENTORY_PATH
        PLAYBOOK_PATH = pipelineParams.PLAYBOOK_PATH
        DEPLOY_ON_K8S = pipelineParams.DEPLOY_ON_K8S
        DEPLOY_APPROVERS = pipelineParams.DEPLOY_APPROVERS
        CHANNEL_TO_NOTIFY = pipelineParams.CHANNEL_TO_NOTIFY
    }
    def securityPermissions = jobConfig.branchProperties

//noinspection GroovyAssignabilityCheck
    pipeline {

        agent { label DEFAULT_NODE_LABEL }

        tools {
            jdk 'Java 8 Install automatically'
            maven 'Maven 3.3.3 Install automatically'
        }

        options {
            timestamps()
            skipStagesAfterUnstable()
            disableConcurrentBuilds()
            authorizationMatrix(inheritanceStrategy: nonInheriting(), permissions: securityPermissions)
        }

        parameters {
            string(name: 'deploy_version', defaultValue: '', description: 'Set artifact version for skip all steps and deploy only \n' +
                    'or leave empty for start full build')
        }

        stages {
            stage('Set additional properties') {
                steps {
                    script {
                        utils = jobConfig.getUtils()
                        jobConfig.setBuildVersion(params.deploy_version)

                        env.APP_NAME = jobConfig.APP_NAME
                        env.INVENTORY_PATH = jobConfig.INVENTORY_PATH
                        env.PLAYBOOK_PATH = jobConfig.PLAYBOOK_PATH
                        env.DEPLOY_ON_K8S = jobConfig.DEPLOY_ON_K8S
                        env.CHANNEL_TO_NOTIFY = jobConfig.CHANNEL_TO_NOTIFY
                        env.DEPLOY_ENVIRONMENT = jobConfig.DEPLOY_ENVIRONMENT
                        env.VERSION = jobConfig.version
                        env.BUILD_VERSION = jobConfig.BUILD_VERSION

                        jobConfig.extraEnvs.each { k, v -> env[k] = v }

                        print("\n\n GLOBAL ENVIRONMENT VARIABLES: \n")
                        sh "printenv"
                        print("\n\n ============================= \n")
                    }
                }
            }
            stage('Unit tests') {
                when {
                    expression { jobConfig.DEPLOY_ONLY ==~ false && !(env.BRANCH_NAME ==~ /^(master)$/) }
                }
                steps {
                    script {
                        utils.runTests(jobConfig.projectFlow)
                    }
                }
            }
            stage('Sonar analyzing') {
                when {
                    expression { jobConfig.DEPLOY_ONLY ==~ false && env.BRANCH_NAME ==~ /^(dev|develop)$/ }
                }
                steps {
                    script {
                        utils.runSonarScanner(jobConfig.BUILD_VERSION)
                    }
                }
            }
            stage('Build') {
                when {
                    expression {
                        jobConfig.DEPLOY_ONLY ==~ false && env.BRANCH_NAME ==~ /^(dev|develop|hotfix\/.+|release\/.+)$/
                    }
                }
                parallel {
                    stage('Publish build artifacts') {
                        steps {
                            script {
                                utils.buildPublish(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.DEPLOY_ENVIRONMENT, jobConfig.projectFlow)
                            }
                        }
                    }
                    stage('Publish docker image') {
                        when {
                            expression { env.BRANCH_NAME ==~ /^(dev|develop)$/ && jobConfig.DEPLOY_ON_K8S ==~ true }
                        }
                        steps {
                            script {
                                buildPublishDockerImage(jobConfig.APP_NAME, jobConfig.BUILD_VERSION)
                            }
                        }
                    }
                }
            }
            stage('Check approvemets for deploy') {
                when {
                    expression { env.BRANCH_NAME ==~ /^(dev|develop|master|release\/.+)$/ }
                }
                steps {
                    script {
                        if (env.BRANCH_NAME ==~ /^(master|release\/.+)$/) {
//TODO: add approve step, check CR step
//                        approve('Deploy on ' + jobConfig.ANSIBLE_ENV + '?', jobConfig.CHANNEL_TO_NOTIFY, jobConfig.DEPLOY_APPROVERS)
                            isApproved = true //    = approve.isApproved()
                        } else {
                            //always approve for dev branch
                            isApproved = true
                        }
                    }
                }
            }
            stage('Deploy') {
                when {
                    expression { env.BRANCH_NAME ==~ /^(dev|develop|master|release\/.+)$/ }
                }
                parallel {
                    stage('Deploy in kubernetes') {
                        when {
                            expression { env.BRANCH_NAME ==~ /^(dev|develop)$/ && jobConfig.DEPLOY_ON_K8S ==~ true }
                        }
                        steps {
                            script {
                                echo("\n\nBUILD_VERSION: ${jobConfig.BUILD_VERSION}\n\n")
                                kubernetes.deploy(jobConfig.APP_NAME, jobConfig.DEPLOY_ENVIRONMENT, 'dev', jobConfig.BUILD_VERSION)
                            }
                        }
                    }
                    stage('Deploy on environment') {
                        when {
                            expression { isApproved ==~ true }
                        }
                        steps {
                            script {
                                runAnsiblePlaybook.releaseManagement(jobConfig.INVENTORY_PATH, jobConfig.PLAYBOOK_PATH, jobConfig.getAnsibleExtraVars())

                                stage('Wait until service is up') {
                                    try {
                                        for (int i = 0; i < jobConfig.healthCheckUrl.size; i++) {
                                            healthCheck(jobConfig.healthCheckUrl[i])
                                        }
                                    }
                                    catch (e) {
                                        error('Service startup failed ' + e)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        post {
            always {
                slackNotify(jobConfig.CHANNEL_TO_NOTIFY)
            }
        }
    }
}
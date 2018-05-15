#!groovy
import static com.nextiva.SharedJobsStaticVars.*
import com.nextiva.*

def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    jobConfig {
        healthCheckMap = pipelineParams.healthCheckMap
        branchPermissionsMap = pipelineParams.branchPermissionsMap
        projectLanguage = pipelineParams.projectLanguage
        ansibleEnvMap = pipelineParams.ansibleEnvMap
        APP_NAME = pipelineParams.APP_NAME
        BASIC_INVENTORY_PATH = pipelineParams.BASIC_INVENTORY_PATH
        PLAYBOOK_PATH = pipelineParams.PLAYBOOK_PATH
        DEPLOY_APPROVERS = pipelineParams.DEPLOY_APPROVERS
        CHANNEL_TO_NOTIFY = pipelineParams.CHANNEL_TO_NOTIFY
    }
    def securityPermissions = jobConfig.branchProperties
    def DEPLOY_ON_SSO_SANDBOX = jobConfig.DEPLOY_ON_K8S

//noinspection GroovyAssignabilityCheck
    pipeline {

        agent { label 'debian' }

        tools {
            jdk 'Java 8 Install automatically'
            maven 'Maven 3.3.3 Install automatically'
        }

        options {
            timestamps()
            skipStagesAfterUnstable()
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
                                error("""Incorrect programming language
                                        please set one of the
                                        supported languages:
                                        java
                                        python
                                        js""")
                                break
                        }
                        utils.setBuildVersion(params.deploy_version)
                        DEPLOY_ONLY = utils.DEPLOY_ONLY
                        DEPLOY_ON_K8S = jobConfig.DEPLOY_ON_K8S
                    }
                }
            }

            stage('Test') {
                when {
                    expression { DEPLOY_ONLY ==~ false && !(env.BRANCH_NAME ==~ /^(master)$/) }
                }
                parallel {
                    stage('Unit tests') {
                        steps {
                            script {
                                utils.test()
                            }
                        }
                    }
                    stage('Sonar analyzing') {
                        steps {
                            script {
                                utils.runSonarScanner('213')
                            }
                        }
                    }
                }
            }
            stage('Build') {
                when {
                    expression { DEPLOY_ONLY ==~ false && env.BRANCH_NAME ==~ /^(dev|develop|hotfix\/.+|release\/.+)$/ }
                }
                parallel {
                    stage('Publish build artifacts') {
                        steps {
                            script {
                                utils.buildPublish()
                            }
                        }
                    }
                    stage('Publish docker image') {
                        steps {
                            script {
                                buildPublishDockerImage(jobConfig.APP_NAME, jobConfig.BUILD_VERSION)
                            }
                        }
                    }
                }
            }
            stage('Deploy') {
                when {
                    expression { env.BRANCH_NAME ==~ /^(dev|develop|master|release\/.+)$/ }
                }
//                script {
//                    if (env.BRANCH_NAME ==~ /^(master|release\/.+)$/) {
//                        approve('Deploy on ' + jobConfig.ANSIBLE_ENV + '?', jobConfig.CHANNEL_TO_NOTIFY, jobConfig.DEPLOY_APPROVERS)
//
//                        isApproved = true //    = approve.isApproved()
//                    } else {
//                        //always approve for dev branch
//                        isApproved = true
//                    }
//                }
                parallel {
                    stage('Deploy in kubernetes') {
                        when {
                            expression { DEPLOY_ON_K8S ==~ true }
                        }
                        steps {
                            script {
                                echo("\n\nBUILD_VERSION: ${BUILD_VERSION}\n\n")
                                kubernetes.deploy(jobConfig.APP_NAME, 'default', 'dev', jobConfig.BUILD_VERSION)
                            }
                        }
                    }
                    stage('Deploy on environment') {
                        when {
                            expression { isApproved ==~ true }
                        }
                        steps {
                            script {
                                runAnsiblePlaybook.releaseManagement(jobConfig.INVENTORY_PATH, jobConfig.PLAYBOOK_PATH, ANSIBLE_EXTRA_VARS)

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
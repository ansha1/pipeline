#!groovy
import static com.nextiva.SharedJobsStaticVars.*
import com.nextiva.*

def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

//    healthCheckMap = pipelineParams.healthCheckMap
//    branchPermissionsMap = pipelineParams.branchPermissionsMap
//    ansibleEnvMap = pipelineParams.ansibleEnvMap.equals(null) ? ansibleEnvMapDefault : pipelineParams.ansibleEnvMap
//    APP_NAME = pipelineParams.APP_NAME
//    BASIC_INVENTORY_PATH = pipelineParams.BASIC_INVENTORY_PATH
//    PLAYBOOK_PATH = pipelineParams.PLAYBOOK_PATH
//    DEPLOY_APPROVERS = pipelineParams.DEPLOY_APPROVERS
//    CHANNEL_TO_NOTIFY = pipelineParams.CHANNEL_TO_NOTIFY
//

    jobConfig {
        healthCheckMap = [dev       : ["http://192.168.51.120:9020/LicenseService/health",
                                       "http://192.168.51.121:9020/LicenseService/health"],
                          qa        : ["http://10.103.50.50:9020/LicenseService/health",
                                       "http://10.103.50.51:9020/LicenseService/health"],
                          production: ["http://192.168.202.129:9020/LicenseService/health",
                                       "http://192.168.202.130:9020/LicenseService/health",
                                       "http://192.168.202.131:9020/LicenseService/health"]]
        branchPermissionsMap = [dev       : ["authenticated"],
                                qa        : ["rdavis", "avelichko", "avama", "skompanets", "sadgaonkar", "mkhunt"],
                                production: ["rdavis", "avelichko", "avama", "skompanets", "sadgaonkar", "mkhunt"]]

        projectLanguage = 'java'

        APP_NAME = 'test-service'
        BASIC_INVENTORY_PATH = 'ansible/role-based_playbooks/inventory/test/'
        PLAYBOOK_PATH = 'ansible/role-based_playbooks/test-service.yml'
        DEPLOY_APPROVERS = 'esakhnyuk,ifishchuk,mvasilets'
        CHANNEL_TO_NOTIFY = 'testchannel'
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
                        utils = jobConfig.getUtils()
                        utils.setBuildVersion(params.deploy_version)
                        DEPLOY_ONLY = jobConfig.DEPLOY_ONLY
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
                                utils.runSonarScanner(utils.version)
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
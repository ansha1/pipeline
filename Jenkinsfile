@Library('pipeline@develop') _
import static com.nextiva.SharedJobsStaticVars.*


def lockableResource = "nextiva-pipeline-test"

properties properties: [
        disableConcurrentBuilds()
]

sourceBranch = (env.BRANCH_NAME ==~ /PR-.*/) ? bitbucket.getSourceBranchFromPr(env.CHANGE_URL) : env.BRANCH_NAME
lock(lockableResource) {
    changeSharedLibBranch(sourceBranch)

    node(DEFAULT_NODE_LABEL) {
        cleanWs()
        try {
            timestamps {
                ansiColor('xterm') {
                    timeout(time: 50, unit: 'MINUTES') {

                        stage('prep downstream repos') {
                            cleanDownstreamRepos()
                        }

                        stage('checkout') {
                            checkout scm
                        }

                        stage('run unit tests') {
                            echo 'unit tests'
                            sh './gradlew clean test'
                        }

                        stage('sonarqube analysing') {
                            echo 'sonarqube analysing'
                            script {
                                sonarScanner('1.0.0')
                            }
                        }

                        stage('run downstream jobs') {
                            runDownstreamJobs()
                        }

                        stage('stop test applications') {
                            cleanupAfterTests()
                        }
                    }
                }
            }
        } catch (e) {
            currentBuild.result = "FAILED"
            throw e
        } finally {
            publishHTML([allowMissing         : true,
                         alwaysLinkToLastBuild: false,
                         keepAll              : true,
                         reportDir            : 'build/reports/tests/test/',
                         reportFiles          : 'index.html',
                         reportName           : 'Test Report',
                         reportTitles         : ''])

            if (env.BRANCH_NAME ==~ /^(PR.+)$/) {
                slack.prOwnerPrivateMessage(env.CHANGE_URL)
            } else {
                slack.sendBuildStatusPrivateMessage(common.getCurrentUserSlackId())
            }
        }
    }
}

@NonCPS
def changeSharedLibBranch(String libBranch) {
    def testFolder = Jenkins.instance.getItemByFullName("nextiva-pipeline-tests")
    testFolder.properties.each {
        if (it instanceof org.jenkinsci.plugins.workflow.libs.FolderLibraries) {
            def libs = it.getLibraries()
            libs.each { i ->
                if (i instanceof org.jenkinsci.plugins.workflow.libs.LibraryConfiguration) {
                    i.setDefaultVersion(libBranch)
                }
            }
        }
    }
    testFolder.save()
    log('pipeline branch changed to ' + libBranch)
}

def runDownstreamJobs() {

    parallel jsReleaseHotfixStartFinish: {
        build job: 'nextiva-pipeline-tests/test-js-pipeline-release-start', parameters: [string(name: 'USER_DEFINED_RELEASE_VERSION', value: '')]
        build job: 'nextiva-pipeline-tests/test-js-pipeline-hotfix-start', parameters: [string(name: 'hotfixVersion', value: '')]
        build job: 'nextiva-pipeline-tests/test-js-pipeline-hotfix-finish'
        build job: 'nextiva-pipeline-tests/test-js-pipeline-release-finish'
    }, javaReleaseHotfixStartFinish: {
        build job: 'nextiva-pipeline-tests/test-java-pipeline-release-start', parameters: [string(name: 'USER_DEFINED_RELEASE_VERSION', value: '')]
        build job: 'nextiva-pipeline-tests/test-java-pipeline-hotfix-start', parameters: [string(name: 'hotfixVersion', value: '')]
        build job: 'nextiva-pipeline-tests/test-java-pipeline-hotfix-finish'
        build job: 'nextiva-pipeline-tests/test-java-pipeline-release-finish'
    }, pythonReleaseHotfixStartFinish: {
        build job: 'nextiva-pipeline-tests/test-python-pipeline-release-start', parameters: [string(name: 'USER_DEFINED_RELEASE_VERSION', value: '')]
        build job: 'nextiva-pipeline-tests/test-python-pipeline-hotfix-start', parameters: [string(name: 'hotfixVersion', value: '')]
        build job: 'nextiva-pipeline-tests/test-python-pipeline-hotfix-finish'
        build job: 'nextiva-pipeline-tests/test-python-pipeline-release-finish'
    }

    parallel jsIntegration: {
        build job: 'nextiva-pipeline-tests/test-js-pipeline/dev', parameters: [string(name: 'deploy_version', value: '')]
    }, pythonLibIntegration: {
        build job: 'nextiva-pipeline-tests/test-python-lib-pipeline/master', parameters: [string(name: 'deploy_version', value: '')]
    }, javaIntegration: {
        build job: 'nextiva-pipeline-tests/test-java-pipeline/dev', parameters: [string(name: 'deploy_version', value: '')]
    }, pythonIntegration: {
        build job: 'nextiva-pipeline-tests/test-python-pipeline/dev', parameters: [string(name: 'deploy_version', value: '')]
    }
}

def cleanDownstreamRepos() {
    repos = ['ssh://git@git.nextiva.xyz:7999/pipeline/test-js-pipeline.git',
             'ssh://git@git.nextiva.xyz:7999/pipeline/test-java-pipeline.git',
             'ssh://git@git.nextiva.xyz:7999/pipeline/test-python-pipeline.git']

    for (repo in repos) {
        sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
            git branch: 'dev', credentialsId: GIT_CHECKOUT_CREDENTIALS, url: repo

            def branchesOutput = sh script: 'git branch -r', returnStdout: true
            def branchesToDelete = branchesOutput.tokenize('\n')
                    .collect({ it.trim() })
                    .findAll({ it ==~ /^origin\/(release|hotfix)\/\d+.\d+(.\d+)?$/ })
                    .collect({ it.trim().replace("origin/", "") })
            branchesToDelete.each {
                log.info("Delete branch ${it}")
                sh "git push --delete origin ${it}"
            }
            cleanWs()
        }
    }
}

def cleanupAfterTests() {
    log.info("Stop applications and delete static assets")
    parallel cleanupJs: {
        common.remoteSh('10.103.50.60', 'rm -rf /data/js-test/dev/*')
        common.remoteSh('10.103.50.61', 'rm -rf /data/js-test/dev/*')
    }, cleanupJava: {
        common.serviceStop('10.103.50.110', 'test-java-pipeline.service')
    }
    // Python Integration test application is deploying to Kubernetes
    //}, cleanupPython: {
    //    common.serviceStop('10.103.50.112', 'test-python-pipeline.uwsgi.service')
    //}
}

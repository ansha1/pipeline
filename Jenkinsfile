@Library('pipelines@PIPELINE_20') _
import static com.nextiva.SharedJobsStaticVars.*


def lockableResource = "nextiva-pipeline-test"

properties properties: [
    disableConcurrentBuilds()
]

sourceBranch = (env.BRANCH_NAME ==~ /PR-.*/) ? bitbucket.getSoruceBranchFromPr(env.CHANGE_URL) : env.BRANCH_NAME
lock(lockableResource) {
    changeSharedLibBranch(sourceBranch)

    node(DEFAULT_NODE_LABEL) {
        cleanWs()
        try {
            timestamps {
                ansiColor('xterm') {
                    timeout(time: 50, unit: 'MINUTES') {

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
                    }
                }
            }
        } catch (e) {
            currentBuild.result = "FAILED"
            throw e
        } finally {
            slackNotify('testchannel')
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
        build 'nextiva-pipeline-tests/test-js-pipeline-hotfix-finish'
        build job: 'nextiva-pipeline-tests/test-js-pipeline-release-finish'
    }, javaReleaseHotfixStartFinish: {
        build job: 'nextiva-pipeline-tests/test-java-pipeline-release-start', parameters: [string(name: 'USER_DEFINED_RELEASE_VERSION', value: '')]
        build job: 'nextiva-pipeline-tests/test-java-pipeline-hotfix-start', parameters: [string(name: 'hotfixVersion', value: '')]
        build 'nextiva-pipeline-tests/test-java-pipeline-hotfix-finish'
        build job: 'nextiva-pipeline-tests/test-java-pipeline-release-finish'
    }, pythonReleaseHotfixStartFinish: {
        build job: 'nextiva-pipeline-tests/test-python-pipeline-release-start', parameters: [string(name: 'USER_DEFINED_RELEASE_VERSION', value: '')]
        build job: 'nextiva-pipeline-tests/test-python-pipeline-hotfix-start', parameters: [string(name: 'hotfixVersion', value: '')]
        build 'nextiva-pipeline-tests/test-python-pipeline-hotfix-finish'
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

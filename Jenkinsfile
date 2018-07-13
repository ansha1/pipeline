@Library('pipeline') _
import static com.nextiva.SharedJobsStaticVars.*


def lockableResource = "nextiva-pipeline-test"

properties properties: [
    disableConcurrentBuilds()
]

sourceBranch = (BRANCH_NAME ==~ /PR-.*/) ? getSoruceBranchFromPr(CHANGE_URL) : env.BRANCH_NAME
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

String getSoruceBranchFromPr(String url) {

    log("Received PR url: ${url}")
    prUrl = url.replaceAll("xyz/projects", "xyz/rest/api/1.0/projects") - "/overview"
    log("Transform Url for access via rest api: ${prUrl}")

    def prResponce = httpRequest authentication: BITBUCKET_JENKINS_AUTH, httpMode: 'GET', url: prUrl
    def props = readJSON text: prResponce.content

    def sourceBranch = props.fromRef.displayId.trim()
    log("SourceBranch: ${sourceBranch}")

    return sourceBranch
}

def runDownstreamJobs() {

    //TODO:add building release start/release finish jobs for this projects before starting multibranch job
    parallel jsReleaseStartFinish: {
        build job: 'nextiva-pipeline-tests/test-js-pipeline-release-start', parameters: [string(name: 'USER_DEFINED_RELEASE_VERSION', value: '')]
        build job: 'nextiva-pipeline-tests/test-js-pipeline-release-finish'
    }, javaReleaseStartFinish: {
        build job: 'nextiva-pipeline-tests/test-java-pipeline-release-start', parameters: [string(name: 'USER_DEFINED_RELEASE_VERSION', value: '')]
        build job: 'nextiva-pipeline-tests/test-java-pipeline-release-finish'
    }, pythonReleaseStartFinish: {
        build job: 'nextiva-pipeline-tests/test-python-pipeline-release-start', parameters: [string(name: 'USER_DEFINED_RELEASE_VERSION', value: '')]
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

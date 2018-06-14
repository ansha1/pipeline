@Library('pipelines@develop') _
import static com.nextiva.SharedJobsStaticVars.*


sourceBranch = (BRANCH_NAME ==~ /PR-.*/) ? getSoruceBranchFromPr(CHANGE_URL) : BRANCH_NAME
changeSharedLibBranch(sourceBranch)

properties properties: [
        disableConcurrentBuilds()
]

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
                        //TODO: add unit tests
                        echo 'unit tests'
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
    print('pipeline branch changed to ' + libBranch)
}

String getSoruceBranchFromPr(String url) {

    print("Received PR url: ${url}")
    prUrl = url.replaceAll("xyz/projects", "xyz/rest/api/1.0/projects") - "/overview"
    print("Transform Url for access via rest api: ${prUrl}")

    def prResponce = httpRequest authentication: BITBUCKET_JENKINS_AUTH, httpMode: 'GET', url: prUrl
    def props = readJSON text: prResponce.content

    def sourceBranch = props.fromRef.displayId.trim()
    print("SourceBranch: ${sourceBranch}")

    return sourceBranch
}

def runDownstreamJobs() {

    //TODO:add building release start/release finish jobs for this projects before starting multibranch job

    parallel  jsIntegration: {
        build job: 'nextiva-pipeline-tests/test-js-pipeline/dev', parameters: [string(name: 'deploy_version', value: '')]
    }, pythonLibIntegration: {
        build job: 'nextiva-pipeline-tests/test-python-lib/master', parameters: [string(name: 'deploy_version', value: '')]
    }
//    }, javaIntegration: {
//        build job: 'nextiva-pipeline-tests/test-java-pipeline/develop', parameters: [string(name: 'deploy_version', value: '')]
//    }
}
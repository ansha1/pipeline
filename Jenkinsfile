@Library('pipelines@develop') _
import static com.nextiva.SharedJobsStaticVars.*

node('slave4') {
    try {
        ansiColor('xterm') {
            timeout(time: 50, unit: 'MINUTES') {
                stage('checkout') {
                    checkout scm
                }

                sourceBranch = getSoruceBranchFromPr(CHANGE_URL)
                changeSharedLibBranch(sourceBranch)

                stage('run downstream jobs') {
                    runDownstreamJobs()
                }
            }
        }
    } catch (e) {
        error(e)
    } finally {
        slackNotify('testchannel')
    }
}

@NonCPS
def changeSharedLibBranch(String libBranch) {
    stage('change pipeline branch in test folder') {
        try {
            if (env.BRANCH_NAME ==~ /^(PR-.*)$/) {

                def testFolder = Jenkins.instance.getItemByFullName("nextiva-pipeline-tests")
                testFolder.properties.each {
                    if (it instanceof org.jenkinsci.plugins.workflow.libs.FolderLibraries) {
                        libs = it.getLibraries()
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
        } catch (e) {
            print(e.toString())
        }
    }
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

    echo 'some output'
//        parallel javaIntegration: {
//            build job: 'nextiva-pipeline-tests/test-java-pipeline/develop', parameters: [string(name: 'deploy_version', value: '')]
//        }, jsIntegration: {
//            build job: 'nextiva-pipeline-tests/test-js-pipeline/develop', parameters: [string(name: 'deploy_version', value: '')]
//        }, pythonLibIntegration: {
//            build job: 'nextiva-pipeline-tests/test-python-client/master', parameters: [string(name: 'deploy_version', value: '')]
//        },
//        failFast: true
}
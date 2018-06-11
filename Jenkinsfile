@Library('pipelines@develop') _
import static com.nextiva.SharedJobsStaticVars.*

node('slave4') {
    try {

        ansiColor('xterm') {
            timeout(time: 50, unit: 'MINUTES') {
                checkout scm
                stage('printenv') {
                    sh 'printenv'
                }

                changeSharedLibBranch()

            }
        }
    } catch (e) {
        error(e)
    } finally {
//        slackNotify('testchannel')
        echo('1')
    }

}

@NonCPS
def changeSharedLibBranch() {
    if (env.BRANCH_NAME ==~ /^(PR-.*)$/) {
        def sourceBranch = getSoruceBranchFromPr(CHANGE_URL)

        stage('change pipeline branch in test folder') {
            def testFolder = Jenkins.instance.getItemByFullName("nextiva-pipeline-tests")
            testFolder.properties.each {
                if (it instanceof org.jenkinsci.plugins.workflow.libs.FolderLibraries) {
                    libs = it.getLibraries()
                    libs.each { i -> print(i.setDefaultVersion(sourceBranch)) }
                }
            }
            testFolder.save()
            print('pipeline branch changed to ' + currentBranch)
        }
    }
}

String getSoruceBranchFromPr(String url) {

    print("Received PR url: ${url}")

//    http://git.nextiva.xyz/projects/REL/repos/pipelines/pull-requests/85/overview
//    http://git.nextiva.xyz/rest/api/1.0/projects/REL/repos/pipelines/pull-requests/85

    prUrl = url.replaceAll("xyz/projects", "xyz/rest/api/1.0/projects") - "/overview"

    print("Transform Url for access via rest api: ${prUrl}")

    def prResponce = httpRequest authentication: BITBUCKET_JENKINS_AUTH, httpMode: 'GET', url: prUrl
    def props = readJSON text: prResponce.content
//    def revs = props.fromRef.displayId.trim()
    sourceBranch = props.fromRef.displayId.trim()
    print("SourceBranch: ${sourceBranch}")

    return sourceBranch
}
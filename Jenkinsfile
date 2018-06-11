@Library('pipelines@develop') _
import static com.nextiva.SharedJobsStaticVars.*

node('slave4') {
    try {

        ansiColor('xterm') {
            timeout(time: 50, unit: 'MINUTES') {
                checkout scm

                changeSharedLibBranch(getSoruceBranchFromPr(CHANGE_URL))

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
    if (env.BRANCH_NAME ==~ /^(PR-.*)$/) {
        stage('change pipeline branch in test folder') {
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
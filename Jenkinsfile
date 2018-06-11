@Library('pipelines@develop') _

node('slave4') {
    try {
        timeout(time: 50, unit: 'MINUTES') {
            checkout scm
            stage('printenv') {
                sh 'printenv'
            }

            changeSharedLibBranch()

        }
    } catch (e) {
        error(e)    } finally {
//        slackNotify('testchannel')
        echo('1')
    }
}

@NonCPS
def changeSharedLibBranch() {
    if (env.BRANCH_NAME ==~ /^(PR-.*)$/) {

        //taking branch name from git because env.BRANCH_NAME is PR-*
        currentBranch = sh returnStdout: true, script: 'git branch -r'
        currentBranch = currentBranch.trim().replace("origin/", "")

        stage('change pipeline branch in test folder') {
            def testFolder = Jenkins.instance.getItemByFullName("nextiva-pipeline-tests")
            testFolder.properties.each {
                if (it instanceof org.jenkinsci.plugins.workflow.libs.FolderLibraries) {
                    libs = it.getLibraries()
                    libs.each { i -> i.setDefaultVersion(currentBranch) }
                }

            }
            testFolder.save()
            print('pipeline branch changed to ' + currentBranch)
        }
    }
}
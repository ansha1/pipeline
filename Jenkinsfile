@Library('pipelines@develop') _

node('debian') {
    try {
        timeout(time: 50, unit: 'MINUTES') {

            changeSharedLibBranch()

            stage('printenv') {
                sh 'printenv'
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
        stage('change default pipeline branch in test folder') {
            def testFolder = Jenkins.instance.getItemByFullName("nextiva-pipeline-tests")
            testFolder.properties.each {
                if (it instanceof org.jenkinsci.plugins.workflow.libs.FolderLibraries) {

                    libs = it.getLibraries()
                    print(libs)

                    libs.each { i -> print(i.setDefaultVersion(env.BRANCH_NAME)) }
                }

            }
            testFolder.save()
            print('pipeline branch changed to ' + env.BRANCH_NAME)
        }
    }
}
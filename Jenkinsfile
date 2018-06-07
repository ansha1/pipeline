node('debian') {
    try {
        timeout(time: 50, unit: 'HOURS') {

            if (BRANCH_NAME ==~ /^(PR-*)$/) {
                stage('change default pipeline branch in test folder') {
                    def testFolder = Jenkins.instance.getItemByFullName("nextiva-pipeline-tests")
                    testFolder.properties.each {
                        if (it instanceof org.jenkinsci.plugins.workflow.libs.FolderLibraries) {
                            libs.each { i -> i.setDefaultVersion(env.BRANCH_NAME) }
                        }
                    }
                    testFolder.save()
                }
            }
            stage('printenv') {
                sh 'printenv'
            }
        }
    } catch (e) {
        print(e)
    } finally {
        slackNotify('testchannel')
    }
}
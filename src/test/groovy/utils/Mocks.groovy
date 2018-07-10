package utils

trait Mocks implements BasePipelineAccessor {
    void mockSendSlack() {
        basePipelineTest.helper.registerAllowedMethod "slackSend", [Map.class], { println 'Slack message mock' }
        basePipelineTest.binding.setVariable 'env', [
                JOB_NAME : 'Job name',
                BUILD_ID : 'Build Id',
                BUILD_URL: 'https://jenkins.nextiva.xyz/jenkins/'
        ]
    }

}
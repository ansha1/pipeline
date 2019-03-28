package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import com.nextiva.slack.MessagesFactory
import utils.Mocks
import utils.Validator

class SlackTest extends BasePipelineTest implements Mocks, Validator {
    def currentBuild
    def env

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += '/'
        super.setUp()

        currentBuild = [rawBuild: mockObjects.job]
        env = [
                JOB_NAME   : 'Job name',
                BUILD_ID   : 'Build Id',
                BUILD_URL  : 'https://jenkins.nextiva.xyz/jenkins/',
                BRANCH_NAME: 'dev',
                NODE_NAME  : 'Debian Slave 3'
        ]

        mockLog()
        attachScript 'log'
        helper.registerAllowedMethod 'sh', [Map.class], { c -> 'commit message' }
        mockSlack()
    }

    def sh(Map map) {
        return "sh command output"
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    @Test
    void send_build_status_notification() {
        def script = loadScript "vars/slack.groovy"
        script.sendBuildStatus 'some_channel'
        checkThatMockedMethodWasExecuted 'slackSend', 1
        printCallStack()
    }

    @Test
    void factory_test() {
        def script = loadScript "vars/slack.groovy"
        println script.toJson(new MessagesFactory(this).buildStatusMessage())
    }

}

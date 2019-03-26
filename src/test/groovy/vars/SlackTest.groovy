package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import com.nextiva.MessagesFactory
import utils.Mocks
import utils.Validator

class SlackTest extends BasePipelineTest implements Mocks, Validator {

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += '/'
        super.setUp()
        mockLog()
        attachScript 'log'
        helper.registerAllowedMethod 'sh', [Map.class], { c -> 'commit message' }
        mockSlack()
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
        println MessagesFactory.buildStatusMessage(this)
    }

}

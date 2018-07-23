package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

class SlackTest extends BasePipelineTest implements Mocks, Validator {

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += '/'
        super.setUp()
        helper.registerAllowedMethod 'sh', [Map.class], { c -> 'commit message' }
        mockSendSlack()
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    @Test
    void send_slack_notification() {
        def script = loadScript "vars/slack.groovy"
        script.call 'some_channel'
        checkThatMockedMethodWasExecuted 'slackSend', 1
        printCallStack()
    }

}


package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

class ApproveTest extends BasePipelineTest implements Mocks, Validator {
    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += '/'
        super.setUp()
        helper.registerAllowedMethod "input", [Map.class], { c -> "Input Response" }
        mockSendSlack()
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    @Test
    void send_approve_message() {
        //This test case is not valid, since we cannot execute code inside the timeout function
        def script = loadScript "vars/approve.groovy"
        script.call 'some channel', 'approve'
        checkThatMockedMethodWasExecuted 'slackSend', 1
        printCallStack()
    }
}

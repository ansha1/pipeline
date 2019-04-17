package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import com.nextiva.slack.MessagesFactory
import utils.Mocks
import utils.Validator

class SlackTest extends BasePipelineTest implements Mocks, Validator {

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += '/'
        super.setUp()

        binding.setVariable('currentBuild', [rawBuild: mockObjects.job])

        mockLog()
        attachScript 'log'
        helper.registerAllowedMethod 'sh', [Map.class], { c -> 'commit message' }
        mockSlack()
        mockMap 'httpRequest'
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    @Test
    void send_message() {
        def script = loadScript "vars/slack.groovy"
        script.sendBuildStatus 'some_channel'
        printCallStack()
    }

}

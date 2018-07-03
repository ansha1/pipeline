package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import utils.Mocks
import utils.Validator

@Ignore
class JobTemplateTest extends BasePipelineTest implements Mocks, Validator {

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += '/'
        super.setUp()
//        helper.registerAllowedMethod "input", [Map.class], { c -> "Input Response" }
//        mockSendSlack()
    }
    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }


    @Test
    void execute_pipeline() {
        def script = loadScript "vars/jobTemplate.groovy"
        script.call
        checkThatMockedMethodWasExecuted 'writeFile', 1
        printCallStack()
    }

}

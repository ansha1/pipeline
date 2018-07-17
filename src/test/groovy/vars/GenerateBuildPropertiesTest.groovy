package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

class GenerateBuildPropertiesTest extends BasePipelineTest implements Validator, Mocks {

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += '/'
        super.setUp()
        mockGenerateBuildProperties()
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    @Test
    void write_property_file() {
        def script = loadScript "vars/generateBuildProperties.groovy"
        script.call 'deployEnvironment', 'version', 'jobName'
        checkThatMockedMethodWasExecuted 'writeFile', 1
        printCallStack()
    }
}

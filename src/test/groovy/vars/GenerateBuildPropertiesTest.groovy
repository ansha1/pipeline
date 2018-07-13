package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import utils.Validator

class GenerateBuildPropertiesTest extends BasePipelineTest implements Validator {

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += '/'
        super.setUp()
        helper.registerAllowedMethod 'readProperties', [Map], { c ->
            [
                    deploy_environment : 'deploy_environment_from_file',
                    some_property      : 'Kappa123',
                    some_other_property: 'Keepo'
            ]
        }
        helper.registerAllowedMethod "sh", [Map], { c -> "sh command output" }
        helper.registerAllowedMethod "writeFile", [Map], { c -> "Write file" }
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

package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

class ArchiveToNexusTest extends BasePipelineTest implements Validator, Mocks {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        binding.setVariable 'params', [:]

        mockEnv()
        attachScript 'generateBuildProperties', 'log'

        helper.registerAllowedMethod "sh", [Map], { c -> "sh command output" }
        helper.registerAllowedMethod "writeFile", [Map], { c -> "Write file" }
        helper.registerAllowedMethod "readProperties", [Map], { return [:] }
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    @Test
    void archive_to_valid_environment() {
        def script = loadScript "vars/archiveToNexus.groovy"
        script.call 'dev', 'assetDir', 'version', 'packageName'
        checkThatMethodWasExecutedWithValue 'sh', '.*upload-file.*'
        printCallStack()
    }

    @Test(expected = IllegalArgumentException)
    void archive_to_not_valid_environment() {
        def script = loadScript "vars/archiveToNexus.groovy"
        script.call 'Keepo', 'assetDir', 'version', 'packageName'
        checkThatMethodWasExecutedWithValue 'sh', '.*upload-file.*', 0
    }
}

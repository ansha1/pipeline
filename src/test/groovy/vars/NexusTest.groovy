package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

class NexusTest extends BasePipelineTest implements Validator, Mocks {

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += '/'
        super.setUp()
        binding.setVariable 'params', [:]
        binding.setVariable 'NEXUS_CURL_CONFIG', 'nexus_curl_config_test'

        mockEnv()
        attachScript 'generateBuildProperties', 'log'

        helper.registerAllowedMethod "sh", [Map], { c -> "sh command output" }
        helper.registerAllowedMethod "writeFile", [Map], { c -> "Write file" }
        helper.registerAllowedMethod "readProperties", [Map], { return [:] }
        helper.registerAllowedMethod "file", [Map], { c -> "Secret file" }
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    @Test
    void push_static_assets_to_valid_environment() {
        def script = loadScript "vars/nexus.groovy"
        script.uploadStaticAssets 'dev', 'assetDir', 'version', 'packageName'
        checkThatMethodWasExecutedWithValue 'curl', '.*upload-file.*', 2
        printCallStack()
    }

    @Test(expected = IllegalArgumentException)
    void push_static_assets_to_not_valid_environment() {
        def script = loadScript "vars/nexus.groovy"
        script.uploadStaticAssets 'Keepo', 'assetDir', 'version', 'packageName'
        checkThatMethodWasExecutedWithValue 'curl', '.*upload-file.*', 0
    }
}

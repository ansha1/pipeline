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
        super.setUp()
        binding.setVariable 'params', [:]
        binding.setVariable 'WORKSPACE', 'WORKSPACE'
        binding.setVariable 'NEXUS_CURL_CONFIG', 'nexus_curl_config_test'

        mockEnv()
        attachScript 'generateBuildProperties', 'log'
        
        helper.registerAllowedMethod "file", [Map], { c -> "Secret file" }
        mockGenerateBuildProperties()
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    @Test
    void push_static_assets_to_valid_environment() {
        def script = loadScript "vars/nexus.groovy"
        attachScript 'common'
        script.uploadStaticAssets 'dev', 'assetDir', 'version', 'packageName', 'pathToSrc'
        checkThatMethodWasExecutedWithValue 'sh', '.*upload-file.*', 2, 2
        printCallStack()
    }

    @Test(expected = IllegalArgumentException)
    void push_static_assets_to_not_valid_environment() {
        def script = loadScript "vars/nexus.groovy"
        script.uploadStaticAssets 'Keepo', 'assetDir', 'version', 'packageName', 'pathToSrc'
        checkThatMethodWasExecutedWithValue 'sh', '.*upload-file.*', 0, 2
    }
}

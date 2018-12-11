package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

class BuildPublishDockerImageTest extends BasePipelineTest implements Validator, Mocks {
    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        binding.setVariable 'WORKSPACE', 'WORKSPACE'
        mockEnv()
        attachScript 'generateBuildProperties'
        mockGenerateBuildProperties()
        mockDocker()
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    @Test
    void build_and_publish() {
        def script = loadScript "vars/buildPublishDockerImage.groovy"
        script.call 'appName', 'buildVersion'
        printCallStack()
        checkThatMethodWasExecutedWithValue 'push', '.*latest.*'
        checkThatMockedMethodWasExecuted 'sh', 5
    }
}

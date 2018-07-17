package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

class DebPackageTest extends BasePipelineTest implements Validator, Mocks {
    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        binding.setVariable 'WORKSPACE', 'WORKSPACE'

        mockEnv()
        mockGenerateBuildProperties()
        mockDocker()
        attachScript 'log', 'generateBuildProperties'
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    @Test
    void build_without_extra_parameters() {
        helper.registerAllowedMethod 'fileExists', [String], {true}
        def script = loadScript "vars/debPackage.groovy"
        script.build 'packageName', 'version', 'deployEnvironment'
        printCallStack()
    }

    @Test
    void build_with_extra_path() {
        helper.registerAllowedMethod 'fileExists', [String], {true}
        def script = loadScript "vars/debPackage.groovy"
        script.build 'packageName', 'version', 'deployEnvironment', 'extra/path'
        printCallStack()
        checkThatMethodWasExecutedWithValue 'dir', '.*extra/path'
    }

    @Test
    void build_with_extra_docker_image() {
        helper.registerAllowedMethod 'fileExists', [String], {true}
        def script = loadScript "vars/debPackage.groovy"
        script.build 'packageName', 'version', 'deployEnvironment', 'extra/path', mocksAdjustment.docker
        printCallStack()
    }
}

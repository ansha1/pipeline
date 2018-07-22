package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import hudson.AbortException
import hudson.model.Result
import org.hamcrest.core.StringContains
import org.junit.Assert
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
        binding.setVariable 'Result', Result
        binding.setVariable 'currentBuild', [rawBuild: [:]]
        helper.registerAllowedMethod 'fileExists', [String], { String s -> return !s.contains('not_existing') }

        mockCommon()
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
        def script = loadScript "vars/debPackage.groovy"
        script.build 'packageName', 'version', 'deployEnvironment'
        printCallStack()
    }

    @Test
    void build_with_extra_path() {
        def script = loadScript "vars/debPackage.groovy"
        script.build 'packageName', 'version', 'deployEnvironment', 'extra/path'
        printCallStack()
        checkThatMethodWasExecutedWithValue 'dir', '.*extra/path'
    }

    @Test
    void build_with_extra_docker_image() {
        def script = loadScript "vars/debPackage.groovy"
        script.build 'packageName', 'version', 'deployEnvironment', 'extra/path', mocksAdjustment.docker
        printCallStack()
    }

    @Test
    void cannot_build_with_not_existing_path() {
        def script = loadScript "vars/debPackage.groovy"
        try {
            script.build 'packageName', 'version', 'deployEnvironment', 'not_existing/path'
            Assert.fail('AbortException is expected')
        } catch (AbortException e) {
            Assert.assertThat(e.getMessage(), StringContains.containsString('not_existing'))
            Assert.assertEquals('Wrong result', Result.ABORTED, binding.currentBuild.rawBuild.result)
        }
        printCallStack()
    }
}

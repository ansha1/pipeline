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
        binding.setVariable 'NEXUS_CURL_CONFIG', 'nexus_curl_config_test'
        binding.setVariable 'Result', Result
        binding.setVariable 'currentBuild', [rawBuild: [:]]
        binding.setVariable 'params', [:]
        helper.registerAllowedMethod 'fileExists', [String], { String s -> return !s.contains('not_existing') }

        attachScript 'log', 'generateBuildProperties', 'nexus'
        mockEnv()
        mockGenerateBuildProperties()
        mockDocker()
        mockMap 'file'
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
        script.build 'packageName', 'version', 'deployEnvironment', 'extra/path', mockObjects.docker
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

    @Test
    void publish_without_extra_parameters() {
        helper.registerAllowedMethod 'sh', [Map], { Map m -> return m.get('returnStatus') == true ? 0 : 'sh command output' }
        def script = loadScript "vars/debPackage.groovy"
        script.publish 'packageName', 'dev'
        printCallStack()
    }

    @Test
    void publish_with_extra_path() {
        helper.registerAllowedMethod 'sh', [Map], { Map m -> return m.get('returnStatus') == true ? 0 : 'sh command output' }
        def script = loadScript "vars/debPackage.groovy"
        script.publish 'packageName', 'dev', 'extra/path'
        checkThatMethodWasExecutedWithValue 'dir', '.*extra/path'
        printCallStack()
    }

    @Test
    void check_error_on_failed_deploy() {
        helper.registerAllowedMethod 'sh', [Map], { Map m -> return m.get('returnStatus') == true ? 1 : 'sh command output' }
        def script = loadScript "vars/debPackage.groovy"
        script.publish 'packageName', 'dev'
        checkThatMockedMethodWasExecuted 'error', 1
        printCallStack()
    }

    @Test(expected = IllegalArgumentException)
    void cannot_publish_with_not_valid_env() {
        def script = loadScript "vars/debPackage.groovy"
        script.publish 'packageName', 'Kappa123'
        printCallStack()
    }


}

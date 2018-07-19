package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import hudson.AbortException
import hudson.model.Result
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

class HotfixStartTest extends BasePipelineTest implements Validator, Mocks {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        binding.setVariable 'currentBuild', [rawBuild: [:]]
        binding.setVariable 'Result', Result
        helper.registerAllowedMethod 'getUtils', [String, String], { loadScript('src/com/nextiva/JavaUtils.groovy') }
        attachScript 'log'
        mockClosure 'pipeline', 'agent', 'options', 'tools', 'stages', 'steps', 'script'
        mockString 'label', 'ansiColor', 'jdk', 'maven'
        mockNoArgs 'timestamps', 'cleanWs'
        mockMap 'timeout', 'git'
        mockMapClosure 'sshagent'
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    @Test
    void hotfix_start() {
        def script = loadScript "vars/hotfixStart.groovy"
        script.call {hotfixVersion = '1.0.1'}
        printCallStack()
        checkThatMethodWasExecutedWithValue 'error', '.*Wrong hotfix version.*', 0
    }

    @Test
    void cannot_start_hotfix_with_wrong_version() {
        def script = loadScript "vars/hotfixStart.groovy"
        script.call {hotfixVersion = '1.0.Kappa'}
        printCallStack()
        checkThatMethodWasExecutedWithValue 'error', '.*Wrong hotfix version.*'
    }

    @Test
    void cannot_start_hotfix_if_branches_already_exist() {
        helper.registerAllowedMethod 'sh', [Map], { Map map ->
            if (map.get('script') ==~ 'git branch -r') {
                return 'origin/hotfix/AN-1485\norigin/hotfix/AN-1481'
            }
            return 'sh command output'
        }
        try {
            def script = loadScript "vars/hotfixStart.groovy"
            script.call {}
            Assert.fail('AbortException is expected')
        } catch (AbortException ignored) {
            printCallStack()
            Assert.assertEquals('Wrong result', Result.ABORTED, binding.currentBuild.rawBuild.result)
        }
    }
}

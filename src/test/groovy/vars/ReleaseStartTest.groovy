package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import hudson.AbortException
import hudson.model.Result
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

class ReleaseStartTest extends BasePipelineTest implements Validator, Mocks {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        binding.setVariable 'currentBuild', [rawBuild: mockObjects.job]
        binding.setVariable 'User', mockObjects.user
        binding.setVariable 'Result', Result
        helper.registerAllowedMethod 'getUtils', [String, String], { loadScript('src/com/nextiva/JavaUtils.groovy') }
        helper.registerAllowedMethod 'httpRequest', [Map], {
            Map m ->
                if (((String) m.get('url')).contains('users.lookupByEmail')) {
                    return [content: 'lookupByEmailResponse']
                } else {
                    return 'httpRequestResponse'
                }
        }
        helper.registerAllowedMethod 'readJSON', [Map], {
            Map m ->
                if (((String) m.get('text')).contains('lookupByEmailResponse')) {
                    return [user: [id: 'userId']]
                } else {
                    return [:]
                }
        }
        attachScript 'log', 'common', 'prometheus'
        mockSlack()
        mockClosure 'pipeline', 'agent', 'options', 'tools', 'stages', 'steps', 'script', 'post',
                'success', 'always'
        mockString 'label', 'ansiColor', 'jdk', 'maven'
        mockNoArgs 'timestamps', 'cleanWs'
        mockMap 'timeout', 'git', 'httpRequest'
        mockMapClosure 'sshagent'
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    @Test
    void release_start() {
        helper.registerAllowedMethod 'sh', [Map], { Map map ->
            if (map.get('script') ==~ 'git branch -r.*') {
                return null
            }
            if (map.get('script') ==~ 'git config remote.origin.url') {
                return 'git@bitbucket.org:nextiva/pipelines.git'
            }
            return 'sh command output'
        }
        def script = loadScript "vars/releaseStart.groovy"
        script.call {
            userDefinedReleaseVersion = '1.0.1'
            repositoryUrl = 'ssh://git@git.nextiva.xyz:7999/~oleksandr.kramarenko/qa_integration.git'
        }
        printCallStack()
        checkThatMockedMethodWasExecuted 'error', 0
        checkThatMockedMethodWasExecuted 'warn', 0
    }

    @Test
    void cannot_start_release_if_branches_already_exist() {
        helper.registerAllowedMethod 'sh', [Map], { Map map ->
            if (map.get('script') ==~ 'git branch -r.*') {
                return 'origin/release/AN-1485'
            }
            return 'sh command output'
        }
        def script = loadScript "vars/releaseStart.groovy"
        try {
            script.call {
                userDefinedReleaseVersion = '1.0.1'
                repositoryUrl = 'ssh://git@git.nextiva.xyz:7999/~oleksandr.kramarenko/qa_integration.git'
            }
            Assert.fail('AbortException is expected')
        } catch (AbortException ignored) {
            printCallStack()
            Assert.assertEquals('Wrong result', Result.ABORTED, binding.currentBuild.rawBuild.result)
        }
    }

}

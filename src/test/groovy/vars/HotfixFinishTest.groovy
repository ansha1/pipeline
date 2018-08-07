package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import hudson.AbortException
import hudson.model.Result
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

class HotfixFinishTest extends BasePipelineTest implements Validator, Mocks {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        binding.setVariable 'currentBuild', [rawBuild: mockObjects.job]
        binding.setVariable 'User', mockObjects.user
        binding.setVariable 'Result', Result
        helper.registerAllowedMethod 'getUtils', [String, String], { loadScript('src/com/nextiva/JavaUtils.groovy') }
        helper.registerAllowedMethod 'readMavenPom', [Map], { [version: '1.0.1'] }
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
        attachScript 'log', 'mergeBranches', 'common'
        mockSlack()
        mockClosure 'pipeline', 'agent', 'options', 'tools', 'stages', 'steps', 'script', 'post',
                'success', 'always'
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
    void hotfix_finish() {
        helper.registerAllowedMethod 'sh', [Map], { Map map ->
            if (map.get('script') ==~ 'git branch -r.*') {
                return 'origin/hotfix/0.0.1\norigin/release/0.0.0'
            }
            return 'sh command output'
        }
        def script = loadScript "vars/hotfixFinish.groovy"
        script.call {
            developBranch = 'develop'
            repositoryUrl = 'ssh://git@git.nextiva.xyz:7999/~oleksandr.kramarenko/qa_integration.git'
        }
        printCallStack()
        checkThatMockedMethodWasExecuted 'warn', 0
        checkThatMockedMethodWasExecuted 'error', 0
    }

    @Test
    void cannot_finish_hotfix_if_not_branches() {
        helper.registerAllowedMethod 'sh', [Map], { Map map ->
            if (map.get('script') ==~ 'git branch -r.*') {
                return ''
            }
            return 'sh command output'
        }
        def script = loadScript "vars/hotfixFinish.groovy"
        try {
            script.call {
                developBranch = 'develop'
                repositoryUrl = 'ssh://git@git.nextiva.xyz:7999/~oleksandr.kramarenko/qa_integration.git'
            }
            Assert.fail('AbortException is expected')
        } catch (AbortException ignored) {
            printCallStack()
            Assert.assertEquals('Wrong result', Result.ABORTED, binding.currentBuild.rawBuild.result)
        }
        checkThatMethodWasExecutedWithValue 'error', '.*There are no hotfix branches.*'
    }

    @Test
    void cannot_finish_hotfix_if_multiple_branches() {
        helper.registerAllowedMethod 'sh', [Map], { Map map ->
            if (map.get('script') ==~ 'git branch -r.*') {
                return 'origin/hotfix/0.0.1\norigin/hotfix/0.0.2'
            }
            return 'sh command output'
        }
        def script = loadScript "vars/hotfixFinish.groovy"
        try {
            script.call {
                developBranch = 'develop'
                repositoryUrl = 'ssh://git@git.nextiva.xyz:7999/~oleksandr.kramarenko/qa_integration.git'
            }
            Assert.fail('AbortException is expected')
        } catch (AbortException ignored) {
            printCallStack()
            Assert.assertEquals('Wrong result', Result.ABORTED, binding.currentBuild.rawBuild.result)
        }
        checkThatMethodWasExecutedWithValue 'error', '.*more then 1 hotfix.*'
    }

    @Test
    void cannot_finish_hotfix_with_not_valid_dev_branch() {
        helper.registerAllowedMethod 'sh', [Map], { Map map ->
            if (map.get('script') ==~ 'git branch -r.*') {
                return 'origin/hotfix/0.0.1\norigin/release/0.0.0'
            }
            return 'sh command output'
        }
        def script = loadScript "vars/hotfixFinish.groovy"
        script.call {
            developBranch = 'developer'
            repositoryUrl = 'ssh://git@git.nextiva.xyz:7999/~oleksandr.kramarenko/qa_integration.git'
        }
        printCallStack()
        checkThatMethodWasExecutedWithValue 'error', '.*Wrong develop branch name.*'
    }

    @Test
    void cannot_finish_hotfix_if_merge_to_master_failed() {
        helper.registerAllowedMethod 'sh', [Map], { Map map ->
            if (map.get('script') ==~ 'git branch -r.*') {
                return 'origin/hotfix/0.0.1'
            }
            return 'sh command output'
        }
        helper.registerAllowedMethod 'sh', [String],
                { String s ->
                    if (s.contains('git checkout master')) {
                        throw new AbortException('script returned exit code 1')
                    } else {
                        return 'sh command output'
                    }
                }
        def script = loadScript "vars/hotfixFinish.groovy"
        try {
            script.call {
                developBranch = 'develop'
                repositoryUrl = 'ssh://git@git.nextiva.xyz:7999/~oleksandr.kramarenko/qa_integration.git'
            }
            printCallStack()
            Assert.fail('AbortException is expected')
        } catch (AbortException ignored) {
            printCallStack()
            Assert.assertEquals('Wrong result', Result.ABORTED, binding.currentBuild.rawBuild.result)
        }
    }

}

package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import hudson.AbortException
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

class MergeBranchesTest extends BasePipelineTest implements Validator, Mocks {
    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        mockCreatePr()
        mockSlack()
        mockMapClosure 'sshagent'
        mockString 'stage'
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    void mockMergeResult(isSoftSucceed = true, isForceSucceed = true) {
        helper.registerAllowedMethod 'sh', [String], { String s ->
            if (s.contains('merge --no-ff')) {
                if (s.contains('-s recursive -Xours')) {
                    if (isForceSucceed) {
                        return 'force merge output'
                    } else {
                        throw new AbortException('script returned exit code 1')
                    }
                } else {
                    if (isSoftSucceed) {
                        return 'soft merge output'
                    } else {
                        throw new AbortException('script returned exit code 1')
                    }
                }
            }
            return 'sh command output'
        }
    }

    void mockUnmergedFiles(files = [], lines = []) {
        helper.registerAllowedMethod 'sh', [Map], { Map map ->
            if (map.get('script') != null) {
                if (map.get('script').toString().contains('git ls-files -u')) {
                    return files.join('\n')
                }
                if (map.get('script').toString().contains('pcregrep')) {
                    return lines.join('\n')
                }
                if (map.get('script').toString().contains('git config --get remote.origin.url')) {
                    return 'ssh://git@git.nextiva.xyz:7999/~oleksandr.kramarenko/qa_integration.git'
                }
            }
            return 'sh command output'
        }
    }

    @Test
    void merge_with_soft_merge() {
        mockMergeResult true
        def script = loadScript "vars/mergeBranches.groovy"
        script.call 'origin/feature/Kappa123', 'origin/develop', 'some_channel'
        printCallStack()
        validateThatSoftMergeWasExecuted()
        validateThatForceMergeWasNotExecuted()
    }

    @Test
    void merge_with_force_merge() {
        mockMergeResult false, true
        mockUnmergedFiles(['pom.xml',
                           'package.json',
                           'build.properties'],
                ['  <version>1.0.0-SNAPSHOT</version>',
                 '  <version>1.0.1-SNAPSHOT</version>',
                 '"version": "1.0.0",',
                 '"version": "1.0.1",',
                 'version=1.5.0',
                 'version=1.5.1'
                ])
        def script = loadScript "vars/mergeBranches.groovy"
        script.call 'origin/feature/Kappa123', 'origin/develop', 'some_channel', false, true
        printCallStack()
        validateThatForceMergeWasExecuted()
        validateThatPrWasNotCreated()
    }

    @Test
    void cannot_merge_force_unexpected_file() {
        mockMergeResult false, true
        mockUnmergedFiles(['kappa.xml',
                           'pom.xml'],
                ['  <version>1.0.0-SNAPSHOT</version>',
                 '  <version>1.0.1-SNAPSHOT</version>',
                ])
        def script = loadScript "vars/mergeBranches.groovy"
        script.call 'origin/feature/Kappa123', 'origin/develop', 'some_channel', false, true
        printCallStack()
        validateThatForceMergeWasNotExecuted()
        validateThatPrWasNotCreated()
        checkThatMethodWasExecutedWithValue 'error', '.*Conflicts in file kappa.xml.*'
    }

    @Test
    void cannot_merge_force_unexpected_lines() {
        mockMergeResult false, true
        mockUnmergedFiles(['pom.xml'],
                ['  <artifactId>Kappa</artifactId>',
                 '  <artifactId>Kappa123</artifactId>',
                ])
        def script = loadScript "vars/mergeBranches.groovy"
        script.call 'origin/feature/Kappa123', 'origin/develop', 'some_channel', false, true
        printCallStack()
        validateThatForceMergeWasNotExecuted()
        checkThatMethodWasExecutedWithValue 'error', '.*Conflicts in line: <artifactId>Kappa.*'
    }

    @Test
    void create_pr_if_auto_merge_is_disabled() {
        mockUnmergedFiles()
        mockMergeResult false, true
        def script = loadScript "vars/mergeBranches.groovy"
        attachScript 'common'
        script.call 'origin/feature/Kappa123', 'origin/develop', 'some_channel', true, false
        printCallStack()
        validateThatForceMergeWasNotExecuted()
        checkThatMethodWasExecutedWithValue 'error', '.*manually with pull request.*'
    }

    @Test
    void create_pr_if_is_not_mergeable() {
        mockMergeResult false, true
        mockUnmergedFiles(['pom.xml'],
                ['  <artifactId>Kappa</artifactId>',
                 '  <artifactId>Kappa123</artifactId>',
                ])
        def script = loadScript "vars/mergeBranches.groovy"
        attachScript 'common'
        script.call 'origin/feature/Kappa123', 'origin/develop', 'some_channel', true, false
        printCallStack()
        validateThatForceMergeWasNotExecuted()
        validateThatPrWasCreated()
    }

    void validateThatSoftMergeWasExecuted() {
        checkThatMethodWasExecutedWithValue 'sh', '.*merge --no-ff.*', 1
    }

    void validateThatForceMergeWasExecuted() {
        checkThatMethodWasExecutedWithValue 'sh', '.*merge --no-ff -s recursive -Xours.*', 1
    }

    void validateThatForceMergeWasNotExecuted() {
        checkThatMethodWasExecutedWithValue 'sh', '.*merge --no-ff -s recursive -Xours.*', 0
    }

    void validateThatPrWasCreated() {
        checkThatMethodWasExecutedWithValue 'error', '.*manually with pull request.*', 1
    }

    void validateThatPrWasNotCreated() {
        checkThatMethodWasExecutedWithValue 'error', '.*manually with pull request.*', 0
    }

}

package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import com.lesfurets.jenkins.unit.MethodCall
import com.nextiva.SharedJobsStaticVars
import com.nextiva.utils.LogLevel
import org.junit.Before
import org.junit.Test
import utils.BranchNames
import utils.JenkinsScriptsHelper
import utils.Mocks
import utils.Validator

import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static com.nextiva.SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
import static org.assertj.core.api.Assertions.assertThat

class NextivaPipelineTest extends BasePipelineTest implements Validator, Mocks, JenkinsScriptsHelper {

    private void branchStepsValidator(Set<String> branchNames, Set<String> requiredSteps) {
        helper.registerAllowedMethod 'fileExists', [String], { s ->
            return s == SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
        }
        helper.registerAllowedMethod 'isPypiPackageExists', [String, String, String], {
            return true
        }
        branchNames.each { branchName ->
            helper.callStack = []
            binding.setVariable 'BRANCH_NAME', branchName
            Script script = loadScriptHelper("simple_python_app.jenkins")
            script.env.BRANCH_NAME = branchName
            runScript(script)
            assertJobStatusSuccess()
            assertThat(helper.callStack.findAll {
                call -> call.methodName == "stage"
            }.collect {
                it.args.first().toString()
            }).describedAs("Branch '$branchName' check").containsExactlyElementsOf(requiredSteps)
        }
    }

    @Override
    @Before
    void setUp() {
        scriptRoots += "src/test/jenkins/jobs/nextivaPipeline"
        super.setUp()

        binding.setVariable 'currentBuild', [result: "SUCCESS", rawBuild: mockObjects.job]
//        binding.setVariable 'User', mockObjects.user
//        binding.setVariable 'NODE_NAME', 'Debian Slave 3'
        binding.setVariable 'WORKSPACE', '/opt/jenkins/workspace/some-workspace'
        binding.setVariable 'BRANCH_NAME', 'dev'
        binding.setVariable 'GIT_URL', 'ssh://git@git.nextiva.xyz:7999/~oleksandr.kramarenko/qa_integration.git'
        binding.setVariable 'params', [
                deploy_version: '1.0',
                stack         : 'a'
        ]

        prepareSharedLib()
    }


    @Test
    void should_execute_without_errors() throws Exception {
        Script script = loadScriptHelper("simple_python_app.jenkins")
        helper.registerAllowedMethod 'fileExists', [String], { s ->
            return s == SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
        }
        runScript(script)
//        printCallStack()
        assertJobStatusSuccess()
    }

    @Test
    void validate_develop_branch_steps() throws Exception {
        branchStepsValidator(BranchNames.develop, BranchNames.developSteps)
    }

    @Test
    void validate_feature_branch_steps() throws Exception {
        branchStepsValidator(BranchNames.feature, BranchNames.featureSteps)
    }

    @Test
    void validate_release_branch_steps() throws Exception {
        branchStepsValidator(BranchNames.release, BranchNames.releaseSteps)
    }

    @Test
    void validate_master_branch_steps() throws Exception {
        branchStepsValidator(BranchNames.master, BranchNames.masterSteps)
    }

    @Test
    void fail_if_build_properties_does_not_exists() throws Exception {
        Script script = loadScriptHelper("simple_python_app.jenkins")
        helper.registerAllowedMethod 'fileExists', [String], { s ->
            if (s == SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME)
                return false
            return false
        }
        runScript(script)
        assertJobStatusFailure()
    }

    @Test
    void skip_dependencies_setup_if_empty() throws Exception {
        Script script = loadScriptHelper("no_depenencies.jenkins")
        helper.registerAllowedMethod 'fileExists', [String], { s ->
            return s == SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
        }
        runScript(script)

        assertThat(helper.callStack.findAll { call ->
            call.methodName == "stage"
        }.any { call ->
            callArgsToString(call).contains("StartBuildDependencies")
        }).isFalse()
        assertJobStatusSuccess()
    }

    @Test
    void can_run_closure_as_build_step() {
        Script script = loadScriptHelper("with_closure.jenkins")
        script.env.BRANCH_NAME = "develop"
        helper.registerAllowedMethod 'fileExists', [String], { s ->
            return s == SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
        }
        runScript(script)

        def closureSignature = { MethodCall it ->
            it.methodName == "stage" && it.argsToString() == "foobar, groovy.lang.Closure"
        }
        List<MethodCall> foobarStages = helper.callStack.findAll(closureSignature)

        assertThat(foobarStages).describedAs("Closure step not found").isNotEmpty()
        assertThat(foobarStages).describedAs("Closure step was executed multiple times").hasSize(1)
        assertThat(helper.callStack.get(
                helper.callStack.findIndexOf(closureSignature) - 3).args[0] == "pip: build"
        ).describedAs("Closure was expected to be in buildCommands").isTrue()
    }

    @Test
    void closure_that_uses_internal_vars() {
        Script script = loadScriptHelper("with_closure.jenkins")
        script.env.BRANCH_NAME = "develop"
        helper.registerAllowedMethod 'fileExists', [String], { s ->
            return s == SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
        }
        runScript(script)
        assertJobStatusSuccess()
        printCallStack()
    }

    @Test
    void docker_only_build() {
        Script script = loadScriptHelper("docker_build_only.jenkins")
        helper.registerAllowedMethod 'fileExists', [String], { s ->
            return s == SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
        }
        helper.registerAllowedMethod 'readProperties', [Map], { Map m ->
            if (m.containsKey("file") && m.get("file") == BUILD_PROPERTIES_FILENAME)
                return ["version": "1.0.1"]
            else
                return null
        }
        runScript(script)

        printCallStack()
        assertJobStatusSuccess()
    }
}

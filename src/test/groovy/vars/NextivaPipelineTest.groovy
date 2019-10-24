package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import com.lesfurets.jenkins.unit.MethodCall
import com.nextiva.SharedJobsStaticVars
import org.junit.Before
import org.junit.Test
import utils.JenkinsScriptsHelper
import utils.Mocks
import utils.Validator

import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static com.nextiva.SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
import static org.assertj.core.api.Assertions.assertThat

class NextivaPipelineTest extends BasePipelineTest implements Validator, Mocks, JenkinsScriptsHelper {

    @Override
    @Before
    void setUp() {
        scriptRoots += "src/test/jenkins/jobs/nextivaPipeline"
        super.setUp()

        binding.setVariable 'currentBuild', [result: "SUCCESS", rawBuild: mockObjects.job]
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

    @Test
    void deployToSandbox() {
        Script script = loadScriptHelper("deploy_to_sandbox.jenkins")
        helper.registerAllowedMethod 'fileExists', [String], { s ->
            return s == SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
        }
        runScript(script)
        assertJobStatusSuccess()

        printCallStack()
        assertThat(helper.callStack.findAll {
            call -> call.methodName == "stage"
        }.collect {
            it.args.first().toString()
        }).describedAs("Check if it is possible to deploy to sandbox").contains("kubeup Deploy: Deploy to sandbox")
    }

    @Test
    void minimumViableBuild() {
//        TODO check that it uses twine, kubeup and docker correctly
        Script script = loadScriptHelper("minimal_python_build.jenkins")
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

        assertJobStatusSuccess()

        assertThat(helper.callStack.findAll {
            call -> call.methodName == "stage"
        }.collect {
            it.args.first().toString()
        }).describedAs("Check that minimum viable Jenkinsfile generates correct steps")
                .containsExactlyElementsOf(["Checkout",
                                            "ConfigureProjectVersion",
                                            "Build",
                                            "pip: build",
                                            "UnitTest",
                                            "pip: unitTest",
                                            "SonarScan",
                                            "Publish",
                                            "pip: publishArtifact",
                                            "Deploy",
                                            "kubeup Deploy: Deploy to dev",
                                            "QACoreTeamTest",
                                            "QA Core Team Tests",
                                            "CollectBuildResults",
                                            "SendNotifications"]
                )
    }
}

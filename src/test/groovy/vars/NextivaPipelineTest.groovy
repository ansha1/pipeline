package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import com.lesfurets.jenkins.unit.MethodCall
import com.nextiva.config.Config
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import utils.JenkinsScriptsHelper
import utils.Mocks
import utils.Validator

import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static com.nextiva.SharedJobsStaticVars.ANSIBLE_NODE_LABEL
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

    @After
    void tearDown() {
        Config.instance.singleInstance = null
    }

    @Test
    void should_execute_without_errors() throws Exception {
        Script script = loadScriptHelper("simple_python_app.jenkins")
        runScript(script)
//        printCallStack()
        assertJobStatusSuccess()
    }


    @Test
    void fail_if_build_properties_does_not_exists() throws Exception {
        Script script = loadScriptHelper("simple_python_app.jenkins")
        helper.registerAllowedMethod 'fileExists', [String], { s ->
            return false
        }
        runScript(script)
        assertJobStatusFailure()
    }

    @Test
    void skip_dependencies_setup_if_empty() throws Exception {
        Script script = loadScriptHelper("no_depenencies.jenkins")
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
        runScript(script)

        def closureSignature = { MethodCall it ->
            it.methodName == "stage" && it.argsToString() == "foobar, groovy.lang.Closure"
        }
        List<MethodCall> foobarStages = helper.callStack.findAll(closureSignature)

        assertThat(foobarStages).describedAs("Closure step not found").isNotEmpty()
        assertThat(foobarStages).describedAs("Closure step was executed multiple times").hasSize(1)
        assertThat(helper.callStack.get(
                helper.callStack.findIndexOf(closureSignature) - 3).args[0] == "python: build"
        ).describedAs("Closure was expected to be in buildCommands").isTrue()
    }

    @Test
    void closure_that_uses_internal_vars() {
        Script script = loadScriptHelper("with_closure.jenkins")
        script.env.BRANCH_NAME = "develop"
        runScript(script)
        assertJobStatusSuccess()
        printCallStack()
    }

    @Test
    void docker_only_build() {
        Script script = loadScriptHelper("docker_build_only.jenkins")
        runScript(script)

        printCallStack()
        assertJobStatusSuccess()
    }

    @Test
    void deployToSandbox() {
        Script script = loadScriptHelper("deploy_to_sandbox.jenkins")
        script.env.BRANCH_NAME = 'feature/dockerTemplate'

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
                                            "python: build",
                                            "UnitTest",
                                            "python: unitTest",
                                            "SonarScan",
                                            "Publish",
                                            "python: publishArtifact",
                                            "Deploy",
                                            "kubeup Deploy: Deploy to dev",
                                            "QACoreTeamTest",
                                            "QA Core Team Tests",
                                            "CollectBuildResults",
                                            "SendNotifications"]
                )
    }

    @Test
    void mavenBuild() {
        Script script = loadScriptHelper("maven_build.jenkins")
        helper.registerAllowedMethod 'readMavenPom', [Map], { ['version': '1.0.1-SNAPSHOT'] }
        helper.registerAllowedMethod "string", [Map], null
        helper.registerAllowedMethod "withCredentials", [List, Object], { l, c ->
            helper.callClosure(c)
        }
        binding.setVariable 'NODE_NAME', ANSIBLE_NODE_LABEL
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
                                            "maven: build",
                                            "UnitTest",
                                            "maven: unitTest",
                                            "SonarScan",
                                            "IntegrationTest",
                                            "maven: integrationTest",
                                            "Publish",
                                            "maven: publishArtifact",
                                            "Deploy",
                                            "kubeup Deploy: Deploy to dev",
                                            "QACoreTeamTest",
                                            "QA Core Team Tests",
                                            "CollectBuildResults",
                                            "SendNotifications"]
                )
    }

    @Test
    void ansibleDeploy() {
        Script script = loadScriptHelper("ansible_deploy.jenkins")
        helper.registerAllowedMethod 'file', [Map], { Map m ->
            if(m.containsKey('credentialsId') && m.credentialsId == "ansible-vault-password-release-management" &&
                    m.containsKey("variable") && m.credentialsId == "ANSIBLE_PASSWORD_PATH") {
                return "ANSIBLE_PASSWORD_PATH"
            }
        }
        runScript(script)

        assertJobStatusSuccess()

        assertThat(helper.callStack.findAll {
            call -> call.methodName == "stage"
        }.collect {
            it.args.first().toString()
        }).describedAs("Ensure that ansible deploy step exists")
                .contains("ansible Deploy: Deploy to dev")

        assertThat(helper.callStack.findAll {
            call -> call.methodName == "runAnsiblePlaybook"
        }.collect {
            it.argsToString()
        }).describedAs("Ensure that runAnsiblePlaybook receives correct parameters")
                .contains("deploy/ansible, analytics_standalone/inventory/analytics/dev, analytics_standalone/api-server.yml, {version=1.0.1}")
    }

    @Test
    @Ignore
    void nodejsStaticBuild() {
        Script script = loadScriptHelper("nodejs_static.jenkins")
        helper.registerAllowedMethod 'readJSON', [Map], { Map m ->
            if (m.containsKey("file") && m.get("file") == "package.json")
                return ["version": "1.0.1"]
            else
                return null
        }
        helper.registerAllowedMethod "string", [Map], null
        helper.registerAllowedMethod "withCredentials", [List, Object], { l, c ->
            helper.callClosure(c)
        }
        binding.setVariable 'NODE_NAME', ANSIBLE_NODE_LABEL
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
                                            "npm: build",
                                            "UnitTest",
                                            "npm: unitTest",
                                            "SonarScan",
                                            "Publish",
                                            "npm: publishArtifact",
                                            "Deploy",
                                            "static Deploy: Deploy to dev",
                                            "Run ansible playbook ansible/role-based_playbooks/static-deploy.yml",
                                            "QACoreTeamTest",
                                            "QA Core Team Tests",
                                            "CollectBuildResults",
                                            "SendNotifications"]
                )
    }

    @Test
    void analyticsSpaDev() {
        analyticsSpaTest("develop",
                ["dev, packages/spa/dist, 1.0.1, analytics-spa"],
                ["Checkout",
                 "ConfigureProjectVersion",
                 "Build",
                 "npm: build",
                 "UnitTest",
                 "npm: unitTest",
                 "SonarScan",
                 "Publish",
                 "npm: publishArtifact",
                 "Deploy",
                 "static Deploy: Deploy to dev",
                 "Run ansible playbook ansible/role-based_playbooks/static-deploy.yml",
                 "QACoreTeamTest",
                 "QA Core Team Tests",
                 "CollectBuildResults",
                 "SendNotifications"]
        )
    }

    @Test
    void analyticsSpaQA() {
        analyticsSpaTest("release/1.0.1",
                ["production, packages/spa/dist, 1.0.1, analytics-spa"],
                ["Checkout",
                 "ConfigureProjectVersion",
                 "VerifyArtifactVersionInNexus",
                 "npm VerifyArtifactVersionInNexus",
                 "Build",
                 "npm: build",
                 "UnitTest",
                 "npm: unitTest",
                 "SonarScan",
                 "Publish",
                 "npm: publishArtifact",
                 "SecurityScan",
                 "Deploy",
                 "static Deploy: Deploy to qa",
                 "Run ansible playbook ansible/role-based_playbooks/static-deploy.yml",
                 "QACoreTeamTest",
                 "QA Core Team Tests",
                 "CollectBuildResults",
                 "SendNotifications"]
        )
    }

    @Test
    void analyticsSpaProd() {
        analyticsSpaTest("master",
                [],
                ["Checkout",
                 "ConfigureProjectVersion",
                 "Deploy",
                 "static Deploy: Deploy to production",
                 "Run ansible playbook ansible/role-based_playbooks/static-deploy.yml",
                 "static Deploy: Deploy to sales-demo",
                 "Run ansible playbook ansible/role-based_playbooks/static-deploy.yml",
                 "QACoreTeamTest",
                 "QA Core Team Tests",
                 "CollectBuildResults",
                 "SendNotifications"]
        )
    }

    void analyticsSpaTest(String branch, List<String> expectedUploadStaticAssetsArgs, List<String> expectedStages) {
        Script script = loadScriptHelper("analytics-spa.jenkins")
        script.env.BRANCH_NAME = branch
        script.binding.BRANCH_NAME = branch
        helper.registerAllowedMethod 'readJSON', [Map], { Map m ->
            if (m.containsKey("file") && m.get("file").endsWith("package.json"))
                return ["version": "1.0.1"]
            else
                return null
        }
        helper.registerAllowedMethod "uploadStaticAssets", [String, String, String, String], { return null }
        binding.setVariable 'NODE_NAME', ANSIBLE_NODE_LABEL
        runScript(script)

        def DEPLOY_ENV = null
        def channelToNotify = null
        switch (branch) {
            case "develop":
                DEPLOY_ENV = 'dev'
                channelToNotify = 'analytics-ci'
                break
            case ~/(release|hotfix)\/.+/:
                DEPLOY_ENV = 'rc'
                channelToNotify = 'analytics'
                break
            case "master":
                DEPLOY_ENV = 'production'
                channelToNotify = 'analytics'
                break
            default:
                channelToNotify = 'analytics-ci'
                DEPLOY_ENV = null
        }
        assertThat(helper.callStack.find{ (it.methodName == "sendBuildStatus") }.argsToString()).isEqualTo(channelToNotify)
        assertThat(script.binding.variables.env).containsAllEntriesOf([
                "CI"     : "true",
                "APP_ENV": DEPLOY_ENV,
        ])

        assertJobStatusSuccess()
        printCallStack()

        assertThat(helper.callStack.findAll {
            call -> call.methodName == "uploadStaticAssets"
        }.collect {
            it.argsToString()
        }).describedAs("Ensure nexus.uploadStaticAssets arguments are correct")
                .isEqualTo(expectedUploadStaticAssetsArgs)

        assertThat(helper.callStack.findAll {
            call -> call.methodName == "stage"
        }.collect {
            it.args.first().toString()
        }).describedAs("Check that minimum viable Jenkinsfile generates correct steps")
                .containsExactlyElementsOf(expectedStages)
    }
}

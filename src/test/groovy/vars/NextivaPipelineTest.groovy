package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import com.lesfurets.jenkins.unit.MethodCall
import com.nextiva.SharedJobsStaticVars
import com.nextiva.utils.LogLevel
import org.junit.Before
import org.junit.Test
import utils.BranchNames
import utils.Mocks
import utils.Validator

import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static org.assertj.core.api.Assertions.assertThat


class NextivaPipelineTest extends BasePipelineTest implements Validator, Mocks {
    def PYTHON_APP = {
        appName = "myapp"
        channelToNotify = "testchannel"

        build = ["pip"   : ["buildCommands"              : "build commands",
                            "postBuildCommands"          : "post Build command",
                            "unitTestCommands"           : "unit test commands",
                            "postUnitTestCommands"       : "post unit test command",
                            "integrationTestCommands"    : "integration test command",
                            "postIntegrationTestCommands": "post integration test commands",
                            "postDeployCommands"         : "post deploy commands",
                            "image"                      : "maven:3.6.1-jdk-8",
                            "resourceRequestCpu"         : "1",
                            "resourceLimitCpu"           : "1",
                            "buildDocker"                : true,
                            "resourceRequestMemory"      : "1Gi",
                            "resourceLimitMemory"        : "1Gi",],
//                TODO docker definition should be optional
                 "docker": ["publishArtifact": true,]]

        deployTool = "kubeup"
        dependencies = ["postgres"                  : "latest",
                        "rabbitmq-ha"               : "latest",
                        "redis-ha"                  : "latest",
                        "rules-engine-core"         : "latest",
                        "rules-engine-orchestration": "latest",]

        environment = ["dev"       : ["healthChecks": ["https://myapp.dev.nextiva.io"]],
                       "qa"        : ["healthChecks"    : ["https://myapp.qa.nextiva.io"],
                                      "ansibleInventory": "rc"],
                       "production": ["healthChecks": ["https://myapp.qa.nextiva.io"]],
                       "sales-demo": ["healthChecks" : ["https://myapp.sales-demo.nextiva.io"],
                                      "branchPattern": /^master$/,]
        ]

        branchPermissions = [:]
    }

    def PYTHON_APP_NO_DEPENDENCIES = {
        def sample_closure = {
            this.script.stage("foobar") { this.script.echo((new Exception().stackTrace.join('\n'))) }
        }
        appName = "myapp"
        channelToNotify = "testchannel"

        build = ["pip"   : ["buildCommands"              : sample_closure,
                            "postBuildCommands"          : """pwd""",
                            "unitTestCommands"           : """cat file.txt""",
                            "postUnitTestCommands"       : """pwd""",
                            "integrationTestCommands"    : """pwd""",
                            "postIntegrationTestCommands": """pwd""",
                            "postDeployCommands"         : """pwd""",
                            "image"                      : "maven:3.6.1-jdk-8",
                            "resourceRequestCpu"         : "1",
                            "resourceLimitCpu"           : "1",
                            "buildDocker"                : true,
                            "resourceRequestMemory"      : "1Gi",
                            "resourceLimitMemory"        : "1Gi",],
                 "docker": ["publishArtifact": true,]]

        deployTool = "kubeup"

        environment = ["dev"       : ["healthChecks": ["https://myapp.dev.nextiva.io"]],
                       "qa"        : ["healthChecks"    : ["https://myapp.qa.nextiva.io"],
                                      "ansibleInventory": "rc"],
                       "production": ["healthChecks": ["https://myapp.qa.nextiva.io"]],
                       "sales-demo": ["healthChecks" : ["https://myapp.sales-demo.nextiva.io"],
                                      "branchPattern": /^master$/,]
        ]

        branchPermissions = [:]
    }

    def script

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
            script.env.BRANCH_NAME = branchName
            script.call PYTHON_APP
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
        scriptRoots += "test/jenkins"
        super.setUp()
        binding.setVariable 'currentBuild', [rawBuild: mockObjects.job]
        binding.setVariable 'User', mockObjects.user
        binding.setVariable 'NODE_NAME', 'Debian Slave 3'
        binding.setVariable 'WORKSPACE', '/opt/jenkins/workspace/some-workspace'
        binding.setVariable 'BRANCH_NAME', 'dev'
        binding.setVariable 'GIT_URL', 'ssh://git@git.nextiva.xyz:7999/~oleksandr.kramarenko/qa_integration.git'
        binding.setVariable 'params', [
                deploy_version: '1.0',
                stack         : 'a'
        ]

        mockMapClosure 'kubernetesSlave'
        attachScript 'jobWithProperties', 'kubernetes', 'log', 'nexus'

        helper.registerAllowedMethod 'waitForQualityGate', [], { [status: 'OK'] }
        helper.registerAllowedMethod 'readMavenPom', [Map], { ['version': '1.0.1'] }
        helper.registerAllowedMethod "choice", [LinkedHashMap], { c -> 'a' }
        helper.registerAllowedMethod "ansiColor", [String, Closure.class], { s, c ->
            Map env = binding.getVariable('env')
            env.put('TERM', s)
            binding.setVariable('env', env)
            c.call()
        }

        helper.registerAllowedMethod "echo", [String], { println it }
        helper.registerAllowedMethod 'readProperties', [Map], { return ["version": "1.0.1"] }

        mockEnv()
        mockDocker()
        mockSlack()

        mockClosure 'pipeline', 'agent', 'tools', 'options', 'stages', 'steps', 'script',
                'when', 'expression', 'parallel', 'post', 'always', 'timestamps'
        mockString 'label', 'jdk', 'maven', 'sh', 'tool', 'ansiColor'
        mockStringStringString 'buildPublishDockerImage', 'buildPublishPypiPackage'
        mockNoArgs 'timestamps', 'nonInheriting'
        mockMap 'authorizationMatrix', 'timeout', 'checkstyle', 'git', 'build', 'slackSend', 'junit',
                'httpRequest', 'booleanParam', 'usernamePassword', 'writeFile'
        mockStringClosure 'dir', 'withSonarQubeEnv', 'lock', 'ansicolor', 'container'
        mockStringStringClosure 'withRegistry'
        mockList 'parameters'
        mockListClosure 'withEnv'
        mockMapClosure 'sshagent', 'withAWS'

        script = loadScript("vars/nextivaPipeline.groovy")
        script.scm = [branches: '', doGenerateSubmoduleConfigurations: '', extensions: '', userRemoteConfigs: '']
        script.binding.getVariable('currentBuild').result = 'SUCCESS'
//        script.env.JOB_LOG_LEVEL = LogLevel.TRACE
        script.env.JOB_LOG_LEVEL = LogLevel.NONE
//        script.env.JOB_LOG_LEVEL = LogLevel.INFO
//        script.env.JOB_LOG_LEVEL = LogLevel.ERROR
    }

    @Test
    void should_execute_without_errors() throws Exception {
        helper.registerAllowedMethod 'fileExists', [String], { s ->
            return s == SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
        }
        script.call PYTHON_APP
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
        helper.registerAllowedMethod 'fileExists', [String], { s ->
            if (s == SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME)
                return false
            return false
        }
        script.call PYTHON_APP
        assertJobStatusFailure()
    }

    @Test
    void skip_dependencies_setup_if_empty() throws Exception {
        helper.registerAllowedMethod 'fileExists', [String], { s ->
            return s == SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
        }
        script.call PYTHON_APP_NO_DEPENDENCIES

        assertThat(helper.callStack.findAll { call ->
            call.methodName == "stage"
        }.any { call ->
            callArgsToString(call).contains("StartBuildDependencies")
        }).isFalse()
        assertJobStatusSuccess()
    }

    @Test
    void can_run_closure_as_build_step() {
        helper.registerAllowedMethod 'fileExists', [String], { s ->
            return s == SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
        }
        script.call PYTHON_APP_NO_DEPENDENCIES

        def closureSignature = { MethodCall it ->
            it.methodName == "stage" && it.argsToString() == "foobar, groovy.lang.Closure"
        }
        List<MethodCall> foobarStages = helper.callStack.findAll(closureSignature)

        assertThat(foobarStages).describedAs("Closure step not found").isNotEmpty()
        assertThat(foobarStages).describedAs("Closure step was executed multiple times").hasSize(1)
        assertThat(helper.callStack.get(
                helper.callStack.findIndexOf(closureSignature) + 1)
                .argsToString().contains("com.nextiva.tools.build.BuildTool.build")
        ).describedAs("Closure was expected to be in buildCommands").isTrue()
    }

    @Test
    void closure_that_uses_internal_vars() {
        helper.registerAllowedMethod 'fileExists', [String], { s ->
            return s == SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
        }
        script.call {
            appName = "foo"
            channelToNotify = "testchannel"
            build = [
                    "pip"   : [
                            "integrationTestCommands"    : """pwd""",
                            "postIntegrationTestCommands": {
                                // this refers to instance of NextivaPipelineTest, while we need an instance of this script
                                def branch = this.script.env.BRANCH_NAME
                                this.script.build job: "/bar/$branch", parameters: [
                                        this.script.string(name: "version", value: getGlobalVersion()),
                                        this.script.string(name: "appName", value: getGlobal().appName)]
                            }
                    ],
                    "docker": ["publishArtifact": true,]
            ]
        }
        assertJobStatusSuccess()
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

}

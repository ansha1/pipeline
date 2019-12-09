package com.nextiva.stages

import com.lesfurets.jenkins.unit.BasePipelineTest
import com.nextiva.SharedJobsStaticVars
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import utils.BranchNames
import utils.JenkinsScriptsHelper
import utils.Mocks
import utils.Validator

import static org.assertj.core.api.Assertions.assertThat

@RunWith(Parameterized.class)
class GitflowProcessTest extends BasePipelineTest implements Validator, Mocks, JenkinsScriptsHelper {

    @Parameterized.Parameter(0)
    public String testSetName

    @Parameterized.Parameter(1)
    public List<String> branchNames

    @Parameterized.Parameter(2)
    public List<String> expectedSteps

    @Parameterized.Parameters(name = "Test {0}")
    static Collection<Object[]> data() {
        return [
                ["feature", BranchNames.feature.toList(),
                 ["Checkout", "StartBuildDependencies", "ConfigureProjectVersion", "Build",
                  "python: build", "UnitTest", "python: unitTest", "IntegrationTest", "python: integrationTest",
                  "CollectBuildResults", "SendNotifications"]],
                ["develop", BranchNames.develop.toList(),
                 ["Checkout", "StartBuildDependencies", "ConfigureProjectVersion", "Build",
                  "python: build", "UnitTest", "python: unitTest", "SonarScan", "IntegrationTest",
                  "python: integrationTest", "Publish", "python: publishArtifact", "Deploy",
                  "kubeup Deploy: Deploy to dev", "python: postDeploy", "QACoreTeamTest",
                  "QA Core Team Tests", "CollectBuildResults", "SendNotifications"]],
                ["master", BranchNames.master.toList(),
                 ["Checkout", "ConfigureProjectVersion", "Deploy", "kubeup Deploy: Deploy to production",
                  "kubeup Deploy: Deploy to sales-demo", "python: postDeploy", "QACoreTeamTest", "QA Core Team Tests",
                  "CollectBuildResults", "SendNotifications"]],
                ["release", BranchNames.release.toList(),
                 ["Checkout", "StartBuildDependencies", "VerifyArtifactVersionInNexus",
                  "python VerifyArtifactVersionInNexus", "docker VerifyArtifactVersionInNexus", "ConfigureProjectVersion",
                  "Build", "python: build", "UnitTest", "python: unitTest", "IntegrationTest", "python: integrationTest",
                  "Publish", "python: publishArtifact", "SecurityScan", "Deploy", "kubeup Deploy: Deploy to qa",
                  "python: postDeploy", "QACoreTeamTest", "QA Core Team Tests", "CollectBuildResults",
                  "SendNotifications"]],
        ].collect { it as Object[] }
    }

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
                deployVersion: '1.0',
                stack        : 'a'
        ]

        prepareSharedLib()

        helper.registerAllowedMethod 'fileExists', [String], { s ->
            return s == SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
        }
        helper.registerAllowedMethod 'isPypiPackageExists', [String, String, String], { return true }
    }

    @Test
    void gitflowBranchStepsTest() {
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
            }).describedAs("Branch '$branchName' check").containsExactlyElementsOf(expectedSteps)
        }
    }
}

package com.nextiva.config

import com.lesfurets.jenkins.unit.BasePipelineTest
import hudson.AbortException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import utils.JenkinsScriptsHelper


import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.hamcrest.CoreMatchers.startsWith

class ConfigTest extends BasePipelineTest implements JenkinsScriptsHelper {

    @Rule
    public ExpectedException thrown = ExpectedException.none()

    PipelineConfig pipelineConfig = [
            appName        : "myapp",
            channelToNotify: "testchannel",
            build          : [
                    "pip"   : [
                            "buildCommands"              : "build commands",
                            "postBuildCommands"          : "post Build command",
                            "unitTestCommands"           : "unit test commands",
                            "postUnitTestCommands"       : "post unit test command",
                            "integrationTestCommands"    : "integration test command",
                            "postIntegrationTestCommands": "post integration test commands",
                            "postDeployCommands"         : "post deploy commands",
                            "image"                      : "python:3.6",
                            "resourceRequestCpu"         : "1",
                            "resourceLimitCpu"           : "1",
                            "buildDocker"                : true,
                            "resourceRequestMemory"      : "1Gi",
                            "resourceLimitMemory"        : "1Gi",
                    ],
                    "docker": [
                            "publishArtifact": true
                    ]
            ]
    ] as PipelineConfig


    @Before
    void setUp() {
        scriptRoots += "src/test/jenkins/jobs/nextivaPipeline"
        super.setUp()
        prepareSharedLib()
        Script script = loadScript("simple_python_app.jenkins")
        binding.setVariable 'env', [
                BRANCH_NAME: 'feature/foo'
        ]
        pipelineConfig.script = script
    }

    @Test
    void testSetDefaults() {
        Config config = Config.getInstance()
        config.copyProperties(pipelineConfig)
        config.setDefaults()
        assertEquals("myapp", config.appName)
        assertEquals("feature/foo", config.branchName)
        assertEquals(true, config.isIntegrationTestEnabled)
    }

    @Test
    void testConfigureDeployTool() {
        Config config = Config.getInstance()
        config.copyProperties(pipelineConfig)
        config.setDefaults()
        config.configureSlave()
        config.configureDeployTool()
        assertTrue(config.isDeployEnabled)
        assertEquals("kubeup", config.deployTool.name)
    }

    @Test
    void configureDeployToolDefaultsToKubeup() {
        Config config = Config.getInstance()
        config.copyProperties(pipelineConfig)
        config.setDefaults()
        config.configureSlave()
        config.configureDeployTool()
        assertTrue(config.isDeployEnabled)
        assertEquals("kubeup", config.deployTool.name)
    }

    @Test
    void configureDeployToolAbortsWithIncorrectToolName() {
        thrown.expect(AbortException)
        thrown.expectMessage(startsWith('Incorrect deploy tool name. Supported tools:'))
        pipelineConfig.deployTool = "SomeWrongTool"
        Config config = Config.getInstance()
        config.copyProperties(pipelineConfig)
        config.setDefaults()
        config.configureSlave()
        config.configureDeployTool()
    }

    @Test
    void configureDeployToolAbortsWithEmptyToolName() {
        thrown.expect(AbortException)
        thrown.expectMessage(startsWith('Incorrect deploy tool name. Supported tools:'))
        pipelineConfig.deployTool = ""
        Config config = Config.getInstance()
        config.copyProperties(pipelineConfig)
        config.setDefaults()
        config.configureSlave()
        config.configureDeployTool()
    }
}

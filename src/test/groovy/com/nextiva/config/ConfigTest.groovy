package com.nextiva.config

import com.lesfurets.jenkins.unit.BasePipelineTest
import hudson.AbortException
import org.apache.commons.io.FileUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import utils.JenkinsScriptsHelper

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource
import static com.nextiva.utils.Utils.getGlobal
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.hamcrest.CoreMatchers.startsWith

class ConfigTest extends BasePipelineTest implements JenkinsScriptsHelper {

    @Rule
    public ExpectedException thrown = ExpectedException.none()

    private Script script
    private pipelineParams = [
            "appName"        : "myapp",
            "channelToNotify": "testchannel"
    ]

    @Before
    void setUp() {
        scriptRoots += "src/test/jenkins/jobs/nextivaPipeline"
        super.setUp()
        prepareSharedLib()
        script = loadScript("simple_python_app.jenkins")
        binding.setVariable 'env', [
                BRANCH_NAME: 'feature/foo'
        ]
    }

    @Test
    void testSetDefaults() {
        def config = new Config(script, pipelineParams)
        config.setDefaults()
        String appName = getGlobal().appName
        assertEquals(appName, "myapp")
    }

    @Test
    void testConfigureDeployTool() {
        def config = new Config(script, pipelineParams)
        config.configureSlave()
        config.configureDeployTool()
        assertTrue(Global.instance.isDeployEnabled)
        assertEquals("kubeup", Global.instance.deployTool.name)
    }

    @Test
    void configureDeployToolDefaultsToKubeup() {
        def config = new Config(script, pipelineParams)
        pipelineParams.remove("deployTool")
        config.configureSlave()
        config.configureDeployTool()
        assertTrue(Global.instance.isDeployEnabled)
        assertEquals("kubeup", Global.instance.deployTool.name)
    }

    @Test
    void configureDeployToolAbortsWithIncorrectToolName() {
        Config config = new Config(script, pipelineParams)
        thrown.expect(AbortException)
        thrown.expectMessage(startsWith('There is no configuration for this tool'))
        pipelineParams.put("deployTool", "SomeWrongTool")
        config.configureSlave()
        config.configureDeployTool()
    }

    @Test
    void configureDeployToolAbortsWithEmptyToolName() {
        Config config = new Config(script, pipelineParams)
        thrown.expect(AbortException)
        thrown.expectMessage(startsWith('There is no configuration for this tool'))
        pipelineParams.put("deployTool", "")
        config.configureSlave()
        config.configureDeployTool()
    }
}

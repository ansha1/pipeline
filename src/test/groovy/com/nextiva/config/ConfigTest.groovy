package com.nextiva.config

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

import static com.nextiva.utils.Utils.getGlobal
import static org.junit.Assert.assertEquals

class ConfigTest extends BasePipelineTest {

    private Script script
    private pipelineParams = [
            "appName"        : "myapp",
            "channelToNotify": "testchannel"
    ]

    @Before
    void setUp() {
        scriptRoots += "src/test/groovy/jenkins"
        super.setUp()
        script = loadScript("jobs/nextivaPipeline")
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

package com.nextiva.tools.build

import com.lesfurets.jenkins.unit.BasePipelineTest
import com.nextiva.config.PipelineConfig
import org.junit.Before
import org.junit.Test
import utils.JenkinsScriptsHelper

import com.nextiva.config.Config

import static org.junit.Assert.assertEquals

class PythonTest extends BasePipelineTest implements JenkinsScriptsHelper {

    private Script script

    @Override
    @Before
    void setUp() {
        scriptRoots += "src/test/jenkins/jobs/nextivaPipeline"
        super.setUp()

        prepareSharedLib()
        script = loadScript("simple_python_app.jenkins")
        PipelineConfig pipelineConfig = new PipelineConfig()
        pipelineConfig.script = script
        Config config = Config.getInstance()
        config.copyProperties(pipelineConfig)
    }

    @Test
    void testGetUnitTestCommands() {
        def defaultUnitTestCommands = """\
                                      pip install -r requirements.txt
                                      pip install -r requirements-test.txt
                                      python setup.py test
                                      """.stripIndent()
        def testData = [
                ["", defaultUnitTestCommands],
                [null, defaultUnitTestCommands],
                [[:], defaultUnitTestCommands],
                [{ 123 }.call(), 123]
        ]
        testData.each { input, result ->

            def pip = new Python(["unitTestCommands": input])
            assertEquals(pip.unitTestCommands, result)
        }
    }

}

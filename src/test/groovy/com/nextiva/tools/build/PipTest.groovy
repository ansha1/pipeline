package com.nextiva.tools.build

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

class PipTest extends BasePipelineTest {

    private Script script

    @Override
    @Before
    void setUp() {
        scriptRoots += "src/test/jenkins"
        super.setUp()
        script = loadScript("jobs/nextivaPipeline")
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
            def pip = new Pip(script, ["unitTestCommands": input])
            assertEquals(pip.unitTestCommands, result)
        }
    }

}

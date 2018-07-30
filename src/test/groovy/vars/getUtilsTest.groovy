package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import static org.junit.Assert.assertEquals;


class getUtilsTest extends BasePipelineTest {


    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += '/'
        super.setUp()
    }

    @Test
    void should_return_javaUtils_class() throws Exception {
        def script = loadScript("vars/getUtils.groovy")
        def responce = script.call('java','.')
        print(responce.getClass())
        assertEquals(responce.getClass().toString(), 'class com.nextiva.JavaUtils')
        printCallStack()
    }

    @Test
    void should_return_jsUtils_class() throws Exception {
        def script = loadScript("vars/getUtils.groovy")
        def responce = script.call('js','.')
        print(responce.getClass())
        assertEquals(responce.getClass().toString(), 'class com.nextiva.JsUtils')
        printCallStack()
    }

    @Test
    void should_return_PythonUtils_class() throws Exception {
        def script = loadScript("vars/getUtils.groovy")
        def responce = script.call('python','.')
        print(responce.getClass())
        assertEquals(responce.getClass().toString(), 'class com.nextiva.PythonUtils')
        printCallStack()
    }
}

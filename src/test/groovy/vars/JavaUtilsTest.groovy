package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

import static org.junit.Assert.assertEquals


class JavaUtilsTest extends BasePipelineTest implements Validator, Mocks {

    static final List MODULES_PROPERTIES_TRUE = [
            ['groupId': 'com.nextiva', 'artifactId': 'calendar-service-parent', 'artifactVersion': '1.5.1', 'packaging': 'jar'],
            ['groupId': 'com.nextiva', 'artifactId': 'calendar-service-common', 'artifactVersion': '1.5.1', 'packaging': 'jar'],
            ['groupId': 'com.nextiva', 'artifactId': 'calendar-service-provider-google', 'artifactVersion': '1.5.1', 'packaging': 'jar'],
            ['groupId': 'com.nextiva', 'artifactId': 'calendar-api-gateway', 'artifactVersion': '1.5.1', 'packaging': 'jar']
    ]

    static final List MODULES_PROPERTIES_FALSE = [
            ['groupId': 'com.nextiva', 'artifactId': 'calendar-service-parent', 'artifactVersion': '1.3.1', 'packaging': 'jar'],
            ['groupId': 'com.nextiva', 'artifactId': 'calendar-service-common', 'artifactVersion': '1.4.0', 'packaging': 'jar'],
            ['groupId': 'com.nextiva', 'artifactId': 'calendar-service-provider-google', 'artifactVersion': '1.5.1', 'packaging': 'jar'],
            ['groupId': 'com.nextiva', 'artifactId': 'calendar-api-gateway', 'artifactVersion': '1.5.1', 'packaging': 'jar']
    ]

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    def getJavaClassObject() {
        def loadJavaClass = loadScript("vars/getUtils.groovy")
        def dummyJavaObject = loadJavaClass.call('java','.')
        dummyJavaObject.metaClass.invokeMethod = helper.getMethodInterceptor()
        dummyJavaObject.metaClass.static.invokeMethod = helper.getMethodInterceptor()
        dummyJavaObject.metaClass.methodMissing = helper.getMethodMissingInterceptor()

        return dummyJavaObject
    }

    @Test
    void check_java_set_version_method() {
        def dummyJavaObject = getJavaClassObject()
        String newVersion = '1.1.1'
        dummyJavaObject.setVersion newVersion
        printCallStack()
        checkThatMethodWasExecutedWithValue 'sh', 'mvn.*', 1, 0

    }

    @Test
    void check_java_create_release_version_method() {
        def dummyJavaObject = getJavaClassObject()
        String newVersion = '1.1.0-SNAPSHOT'
        def response = dummyJavaObject.createReleaseVersion newVersion
        assertEquals(response, '1.1.0')
        printCallStack()
    }

    @Test
    void check_if_maven_artifact_versions_identical() {
        def dummyJavaObject = getJavaClassObject()
        boolean response = dummyJavaObject.isMavenArtifactVersionsEqual MODULES_PROPERTIES_TRUE
        assertEquals(response, true)
        printCallStack()
    }

    @Test
    void check_if_maven_artifact_versions_not_identical() {
        def dummyJavaObject = getJavaClassObject()
        boolean response = dummyJavaObject.isMavenArtifactVersionsEqual MODULES_PROPERTIES_FALSE
        assertEquals(response, false)
        printCallStack()
    }

    @Test
    void check_get_modules_properties() {
        def dummyJavaObject = getJavaClassObject()
        dummyJavaObject.log = ['info': {}, 'debug': {}]
        dummyJavaObject.sh = helper.registerAllowedMethod 'sh', [Map], { c ->
            "com.nextiva calendar-api-gateway 1.3.1 jar\ncom.nextiva calendar-service-provider-google 1.3.1 jar"
        }
        List response = dummyJavaObject.getModulesProperties()
        assertEquals(response[0], ['groupId':'com.nextiva', 'artifactVersion':'1.3.1', 'artifactId':'calendar-api-gateway', 'packaging':'jar'])
        printCallStack()
    }

    @Test
    void check_verify_package_in_nexus_returns_true() {
        def dummyJavaObject = getJavaClassObject()
        dummyJavaObject.log = ['error': {}]
        dummyJavaObject.metaClass.getModulesProperties = { -> MODULES_PROPERTIES_TRUE }
        dummyJavaObject.metaClass.getVersion = { -> '1.5.1' }
        dummyJavaObject.nexus = ['isJavaArtifactExists': { groupId, artifactId, artifactVersion, packaging -> true }]

        def response = dummyJavaObject.verifyPackageInNexus "", "1.5.1", ""
        assertEquals(response, true)
        printCallStack()
    }

    @Test
    void check_verify_package_in_nexus_returns_false_case_1() {
        def dummyJavaObject = getJavaClassObject()
        dummyJavaObject.log = ['error': {}]
        dummyJavaObject.metaClass.getModulesProperties = { -> MODULES_PROPERTIES_TRUE }
        dummyJavaObject.metaClass.getVersion = { -> '1.5.1' }
        dummyJavaObject.nexus = ['isJavaArtifactExists': { groupId, artifactId, artifactVersion, packaging -> false }]

        def response = dummyJavaObject.verifyPackageInNexus "", "1.5.1", ""
        assertEquals(response, false)
        printCallStack()
    }

    @Test
    void check_verify_package_in_nexus_returns_false_case_2() {
        def dummyJavaObject = getJavaClassObject()
        dummyJavaObject.log = ['error': {}]
        dummyJavaObject.metaClass.getModulesProperties = { -> MODULES_PROPERTIES_FALSE }
        dummyJavaObject.metaClass.getVersion = { -> '1.5.1' }
        dummyJavaObject.nexus = ['isJavaArtifactExists': { groupId, artifactId, artifactVersion, packaging -> false }]

        def response = dummyJavaObject.verifyPackageInNexus "", "1.5.1", ""
        assertEquals(response, false)
        printCallStack()
    }

    @Test(expected = hudson.AbortException)
    void check_verify_package_in_nexus_returns_exception_due_to_not_identical_versions() {
        def dummyJavaObject = getJavaClassObject()
        dummyJavaObject.Result = ['ABORTED': 'Build should be aborted']
        dummyJavaObject.currentBuild = ['rawBuild': ['result': 'undefined']]
        dummyJavaObject.log = ['error': {}]
        dummyJavaObject.metaClass.getModulesProperties = { -> MODULES_PROPERTIES_FALSE }
        dummyJavaObject.metaClass.getVersion = { -> '1.5.1' }
        dummyJavaObject.nexus = ['isJavaArtifactExists': { groupId, artifactId, artifactVersion, packaging -> true }]

        def response = dummyJavaObject.verifyPackageInNexus "", "1.5.1", ""
        printCallStack()
    }
}

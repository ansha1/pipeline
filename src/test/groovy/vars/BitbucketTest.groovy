package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

import static com.nextiva.SharedJobsStaticVars.BITBUCKET_SECTION_MARKER

class BitbucketTest extends BasePipelineTest implements Validator, Mocks {
    private static final String PR_URL = "https://git.nextiva.xyz/users/oleksandr.kramarenko/" +
            "repos/qa_integration/pull-requests/1"

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        mockCreatePr(PR_URL)
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    @Test
    void create_pr() {
        def script = loadScript "vars/bitbucket.groovy"
        def url = script.createPr 'ssh://git@git.nextiva.xyz:7999/~oleksandr.kramarenko/qa_integration.git', 'sourceBranch', 'destinationBranch', 'title', 'description'
        printCallStack()
        checkThatMethodWasExecutedWithValue 'info', '.*' + PR_URL + '.*'
        Assert.assertEquals('Wrong pr url', PR_URL, url)
    }

    @Test
    void default_description_conversion() {
        check_description_conversion "Some default description"
    }

    @Test
    void multiline_default_description_conversion() {
        check_description_conversion """First line
Second line"""
    }

    @Test
    void one_section_description_conversion() {
        check_description_conversion """${BITBUCKET_SECTION_MARKER}section name
Some section info
"""
    }

    @Test
    void section_with_multiline_description_conversion() {
        check_description_conversion """${BITBUCKET_SECTION_MARKER}section name
First line
Second line
"""
    }

    @Test
    void default_and_custom_section_description_conversion() {
        check_description_conversion """Some default description
${BITBUCKET_SECTION_MARKER}section name
First line
Second line
"""
    }

    @Test
    void multiple_custom_sections_description_conversion() {
        check_description_conversion """Some default description
${BITBUCKET_SECTION_MARKER}section name
First line
Second line
${BITBUCKET_SECTION_MARKER}other section name
Third line
Fourth line
"""
    }

    private void check_description_conversion(String originalDescription) {
        def script = loadScript "vars/bitbucket.groovy"

        def descriptionMap = script.parseDescription originalDescription
        def convertedDescription = script.descriptionToString descriptionMap

        printCallStack()

        Assert.assertEquals 'Description was corrupted after conversion', originalDescription, convertedDescription
    }
}

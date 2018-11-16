package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

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
}

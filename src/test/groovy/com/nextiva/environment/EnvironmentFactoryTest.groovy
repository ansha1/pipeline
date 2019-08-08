package com.nextiva.environment

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

import static com.nextiva.config.Global.instance as global

class EnvironmentFactoryTest extends BasePipelineTest {
    @Override
    @Before
    void setUp() {
        scriptRoots += "test/jenkins"
        super.setUp()
    }

    @Test
    void testGetAvailableEnvironmentsForBranch() {
        global.branchName = "dev"
        EnvironmentFactory environmentFactory = new EnvironmentFactory([:])
        def envs = environmentFactory.getAvailableEnvironmentsForBranch(global.branchName)
        assertTrue(!envs.isEmpty())
    }
}

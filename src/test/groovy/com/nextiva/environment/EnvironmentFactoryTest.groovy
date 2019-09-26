package com.nextiva.environment


import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import utils.BranchNames

import static org.assertj.core.api.Assertions.assertThat


@RunWith(Parameterized.class)
class EnvironmentFactoryTest {
    @Parameterized.Parameter(0)
    public String branchingModel

    @Parameterized.Parameter(1)
    public String branchType

    @Parameterized.Parameter(2)
    public String branchName

    @Parameterized.Parameter(3)
    public List<String> expectedResult

    @Parameterized.Parameter(4)
    public Map customConfiguration

    static Map cfg1 = [
            "environment": ["dev"       : ["healthChecks": ["https://myapp.dev.nextiva.io"]],
                            "qa"        : ["healthChecks"    : ["https://myapp.qa.nextiva.io"],
                                           "ansibleInventory": "rc"],
                            "production": ["healthChecks": ["https://myapp.qa.nextiva.io"]],
                            "sales-demo": ["healthChecks" : ["https://myapp.sales-demo.nextiva.io"],
                                           "branchPattern": /^master$/,]
            ]
    ]

    @Parameterized.Parameters(name = "{0} - {1} - {2}")
    static Collection<Object[]> data() {
        return [
                BranchNames.feature.collect { ["gitflow", "feature", it, [], [:]] },
                BranchNames.develop.collect { ["gitflow", "develop", it, ["dev"], [:]] },
                BranchNames.release.collect { ["gitflow", "release", it, ["qa"], [:]] },
                BranchNames.master.collect { ["gitflow", "master", it, ["prod"], [:]] },
                (BranchNames.feature + BranchNames.release + BranchNames.develop).collect {
                    ["trunkbased", "not master", it, [], [:]]
                },
                BranchNames.master.collect { ["trunkbased", "master", it, ["dev"], [:]] },
                BranchNames.feature.collect { ["gitflow", "feature", it, [], cfg1] },
                BranchNames.develop.collect { ["gitflow", "develop", it, ["dev"], cfg1] },
                BranchNames.release.collect { ["gitflow", "release", it, ["qa"], cfg1] },
                BranchNames.master.collect { ["gitflow", "master", it, ["prod", "sales-demo"], cfg1] },
                (BranchNames.feature + BranchNames.release + BranchNames.develop).collect {
                    ["trunkbased", "not master", it, [], cfg1]
                },
                BranchNames.master.collect { ["trunkbased", "master", it, ["dev", "sales-demo"], cfg1] },
        ].collectMany { it }.collect { it as Object[] }
    }

    List testDataGenerator(String branchingModel, String branchType, Set branchNames, List expectedResult, Map pipelineConfiguration) {
        return branchNames.collect { [branchingModel, branchType, it, expectedResult, pipelineConfiguration] }
    }

    @Test
    void test_getAvailableEnvironmentsForBranch() {
        EnvironmentFactory environmentFactory = new EnvironmentFactory(customConfiguration)
        def envs = environmentFactory.getAvailableEnvironmentsForBranch(branchName, branchingModel)
        assertThat(envs.collect { it.name })
                .describedAs("Checking $branchType branch pattern of $branchingModel strategy")
                .containsOnlyElementsOf(expectedResult)
    }
}

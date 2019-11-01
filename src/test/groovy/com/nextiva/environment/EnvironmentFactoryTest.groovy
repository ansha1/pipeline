package com.nextiva.environment

import com.nextiva.config.Branch
import com.nextiva.config.BranchingModel
import com.nextiva.config.GitFlow
import com.nextiva.config.TrunkBased
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import utils.BranchNames

import static org.assertj.core.api.Assertions.assertThat


@RunWith(Parameterized.class)
class EnvironmentFactoryTest {
    @Parameterized.Parameter(0)
    public String branchingModelName

    @Parameterized.Parameter(1)
    public String branchType

    @Parameterized.Parameter(2)
    public String branchName

    @Parameterized.Parameter(3)
    public List<String> expectedResult

    @Parameterized.Parameter(4)
    public List<Environment> customConfiguration

    static List<Environment> cfg1 = [
            ["name"        : "dev",
             "healthChecks": ["https://myapp.dev.nextiva.io"]],
            ["name"            : "qa",
             "healthChecks"    : ["https://myapp.qa.nextiva.io"],
             "ansibleInventory": "rc"],
            ["name"        : "production",
             "healthChecks": ["https://myapp.qa.nextiva.io"]],
            ["name"         : "sales-demo",
             "branchPattern": /^master$/,
             "healthChecks" : ["https://myapp.sales-demo.nextiva.io"]]
    ].collect { it as Environment }

    static List<Environment> cfg2 = [
            ["name"        : "dev",
             "healthChecks": ["https://myapp.dev.nextiva.io"]],
            ["name"            : "qa",
             "healthChecks"    : ["https://myapp.qa.nextiva.io"],
             "ansibleInventory": "rc"],
            ["name"        : "production",
             "healthChecks": ["https://myapp.qa.nextiva.io"]],
            ["name"         : "sales-demo",
             "branchPattern": ~/^master$/,
             "healthChecks" : ["https://myapp.sales-demo.nextiva.io"]],
            ["name"             : "nextiva-pipeline-sandbox",
             "branchPattern"    : '^feature/dockerTemplate$',
             "kubernetesCluster": "nextiva-pipeline-sandbox.nextiva.io"],
    ].collect { it as Environment }

    @Parameterized.Parameters(name = "{0} - {1} - {2}")
    static Collection<Object[]> data() {
        return [
                BranchNames.feature.collect { ["gitflow", "feature", it, [], []] },
                BranchNames.develop.collect { ["gitflow", "develop", it, ["dev"], []] },
                BranchNames.release.collect { ["gitflow", "release", it, ["qa"], []] },
                BranchNames.master.collect { ["gitflow", "master", it, ["production"], []] },
                (BranchNames.feature + BranchNames.release + BranchNames.develop).collect {
                    ["trunkbased", "not master", it, [], []]
                },
                BranchNames.master.collect { ["trunkbased", "master", it, ["dev"], []] },
                BranchNames.feature.collect { ["gitflow", "feature", it, [], cfg1] },
                BranchNames.develop.collect { ["gitflow", "develop", it, ["dev"], cfg1] },
                BranchNames.release.collect { ["gitflow", "release", it, ["qa"], cfg1] },
                BranchNames.master.collect { ["gitflow", "master", it, ["production", "sales-demo"], cfg1] },
                (BranchNames.feature + BranchNames.release + BranchNames.develop).collect {
                    ["trunkbased", "not master", it, [], cfg1]
                },
                BranchNames.master.collect { ["trunkbased", "master", it, ["dev", "sales-demo"], cfg1] },

                BranchNames.feature.collect { ["gitflow", "feature", it, [], cfg2] },
                BranchNames.develop.collect { ["gitflow", "develop", it, ["dev"], cfg2] },
                BranchNames.release.collect { ["gitflow", "release", it, ["qa"], cfg2] },
                BranchNames.master.collect { ["gitflow", "master", it, ["production", "sales-demo"], cfg2] },
                (BranchNames.feature + BranchNames.release + BranchNames.develop).collect {
                    ["trunkbased", "not master", it, [], cfg2]
                },
                BranchNames.master.collect { ["trunkbased", "master", it, ["dev", "sales-demo"], cfg2] },
                [["gitflow", "feature/dockerTemplate", "feature/dockerTemplate", ["nextiva-pipeline-sandbox"], cfg2],
                 ["trunkbased", "feature/dockerTemplate", "feature/dockerTemplate", ["nextiva-pipeline-sandbox"], cfg2]],
        ].collectMany { it }.collect { it as Object[] }
    }

    @Test
    void testGetAvailableEnvironmentsForBranch() {
        BranchingModel branchingModel
        if (branchingModelName == "gitflow") {
            branchingModel = new GitFlow()
        } else if (branchingModelName == "trunkbased") {
            branchingModel = new TrunkBased()
        }

        EnvironmentFactory environmentFactory = new EnvironmentFactory(customConfiguration)
        def envs = environmentFactory.getAvailableEnvironmentsForBranch(branchingModel, branchName)
        assertThat(envs.collect { it.name })
                .describedAs("Checking $branchType branch pattern of $branchingModel strategy")
                .containsOnlyElementsOf(expectedResult)
    }
}

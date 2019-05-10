package com.nextiva.branch.model

class GitFlow implements BranchingModel {

    // Stages for tests default executing flow for feature/ PR/ branch types
    private final List<String> test = ["Checkout", "UnitTest", "SonarScan", "IntegrationTest", "SendNotifications"]

    // Tests + Publish + Deploy artifacts for dev release branch types
    private final List<String> testPublishDeploy = ["VerifyArtifactVersionInNexus", "BuildArtifact", "BuildDockerImage", "PublishArtifact", "PublishDockerImage",
                                                    "SecurityScan""]
    // Stages for master or deploy only execute strategy
    private final List<String> deploy = ["Checkout", "Deploy", "PostDeploy", "QACoreTeamTest", "SendNotifications"]

    @Override
    String getRepository(String branchName) {
        switch (branchName) {
            case ~/^master|hotfix\/.+|release\/.+$/:
                return "production"
        }
        return null
    }

    @Override
    List getAllowedEnvs(String branchName) {
        return null
    }

    @Override
    List getStages(String branchName) {
        List<String> flow
        switch (branchName) {
            case ~/(dev|develop)$/:
                flow =
                break
            case ~/^release\/.+$/:
                flow =
                break
            case "master":
                break
            case ~/^hotfix\/.+$/:
                break
            default:
                flow = testStages
                break
        }
        return flow
    }

    @Override
    String getName() {
        return getClass().getSimpleName()
    }
}





testStagesSet
buildPublishStagesSet

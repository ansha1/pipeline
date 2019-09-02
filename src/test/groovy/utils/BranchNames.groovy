package utils

class BranchNames {
    public static final Set<String> feature = ['feature/foobar', 'feature', 'foobar', 'release', 'hotfix', 'hrelease', 'f-dev', 'dev-',
                                               'development', 'aaaarelease/123', 'd-release/10.0.0', '-release/foobar']
    public static final Set<String> release = ['release/123', 'release/1.0.0', 'release/0.0.0', 'release/0.0.0-SNAPSHOT',
                                               'hotfix/123', 'hotfix/1.0.0', 'hotfix/0.0.0', 'hotfix/0.0.0-SNAPSHOT']
    public static final Set<String> develop = ['dev', 'develop']
    public static final Set<String> master = ['master']

    public static final Set<String> featureSteps = [
            "Checkout",
            "StartBuildDependencies",
            "ConfigureProjectVersion",
            "Build",
            "pip: build",
            "UnitTest",
            "pip: unitTest",
            "IntegrationTest",
            "pip: integrationTest",
            "CollectBuildResults",
            "SendNotifications"
    ]
    public static final Set<String> developSteps = [
            "Checkout",
            "StartBuildDependencies",
            "ConfigureProjectVersion",
            "Build",
            "pip: build",
            "UnitTest",
            "pip: unitTest",
            "SonarScan",
            "IntegrationTest",
            "pip: integrationTest",
            "Publish",
            "pip: publishArtifact",
            "Deploy",
            "kubeup Deploy: Deploy to dev",
            "pip: postDeploy",
            "QACoreTeamTest",
            "QA Core Team Tests",
            "CollectBuildResults",
            "SendNotifications"
    ]
    public static final Set<String> releaseSteps = [
            "Checkout",
            "StartBuildDependencies",
            "VerifyArtifactVersionInNexus",
            "pip VerifyArtifactVersionInNexus",
            "docker VerifyArtifactVersionInNexus",
            "ConfigureProjectVersion",
            "Build",
            "pip: build",
            "UnitTest",
            "pip: unitTest",
            "IntegrationTest",
            "pip: integrationTest",
            "Publish",
            "pip: publishArtifact",
            "SecurityScan",
            "Deploy",
            "kubeup Deploy: Deploy to qa",
            "pip: postDeploy",
            "QACoreTeamTest",
            "QA Core Team Tests",
            "CollectBuildResults",
            "SendNotifications"
    ]
    public static final Set<String> masterSteps = [
            "Checkout",
            "ConfigureProjectVersion",
            "IntegrationTest",
            "pip: integrationTest",
            "Deploy",
            "kubeup Deploy: Deploy to prod",
            "kubeup Deploy: Deploy to sales-demo",
            "pip: postDeploy",
            "QACoreTeamTest",
            "QA Core Team Tests",
            "CollectBuildResults",
            "SendNotifications"
    ]
}
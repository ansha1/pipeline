package com.nextiva.stages


abstract class BasicStage implements Serializable {

    final protected script
    final protected Map configuration
    static List GENERAL_BUILD_STEPS_WITH_PREDEFINED_ORDER = [
            'Checkout',
            'VerifyArtifactVersionInNexus',
            'UnitTest',
            'SonarScan',
            'BuildArtifact',
            'BuildDockerImage',
            'PublishArtifact',
            'PublishDockerImage',
            'SecurityScan',
            'IntegrationTest',
            'DeployToK8s',
            'DeployByAnsible',
            'Healthcheck',
            'PostDeploy',
            'QACoreTeamTest',
            'SendNotifications'
    ]


    protected BasicStage(script, configuration) {
        this.script = script
        this.configuration = configuration
    }

    static BasicStage loadConfiguration(script, configuration) {

        List<BasicStage> jobFlowInstancesList = []
        Boolean deployOnly = configuration.get('deployOnly')
        String branchName = configuration.get('branchName')
        String branchingModel = configuration.get('branchingModel')
        Boolean isSecurityScanEnabled = configuration.get('isSecurityScanEnabled')
        Boolean buildDockerImage = configuration.get('buildDockerImage')
        Boolean buildArtifact = configuration.get('buildArtifact')
        Boolean publishArtifact = configuration.get('publishArtifact')
        Boolean publishDockerImage = configuration.get('publishDockerImage')
        Boolean isSonarAnalysisEnabled = configuration.get('isSonarAnalysisEnabled')
        Boolean ansibleDeployment = configuration.get('ansibleDeployment')
        Boolean deployOnK8s = configuration.get('deployOnK8s')
        String postdeploycommands = configuration.get('postdeploycommands')


        for (String buildStep in GENERAL_BUILD_STEPS_WITH_PREDEFINED_ORDER) {
            switch (buildStep) {
                case 'Checkout':
                    if (deployOnly) break
                    jobFlowInstancesList.add(new Checkout(script, configuration))
                    break
                case 'VerifyArtifactVersionInNexus':
                    if (deployOnly) break
                    if ((branchingModel == BranchingModel.GITFLOW.name() && branchName ==~ /^((hotfix|release)\/.+)$/) || (branchingModel == BranchingModel.TRUNCKBASED.name() && branchName == "master")) {
                        jobFlowInstancesList.add(new VerifyArtifactVersionInNexus(script, configuration))
                    }
                    break
                case 'UnitTest':
                    if (deployOnly) break
                    if (branchName == 'master' && branchingModel == BranchingModel.GITFLOW.name()) break
                    jobFlowInstancesList.add(new UnitTest(script, configuration))
                    break
                case 'SonarScan':
                    if (deployOnly) break
                    if (!isSonarAnalysisEnabled) break
                    if ((branchName ==~ /^(develop|dev)$/ && branchingModel == BranchingModel.GITFLOW.name()) || (branchingModel == BranchingModel.TRUNCKBASED.name() && branchName == "master")) {
                        jobFlowInstancesList.add(new SonarScan(script, configuration))
                    }
                    break
                case 'BuildArtifact':
                    if (deployOnly) break
                    if (!buildArtifact) break
                    if ((branchName ==~ /^(dev|develop|hotfix\/.+|release\/.+)$/ && branchingModel == BranchingModel.GITFLOW.name()) || (branchName == "master" && branchingModel == BranchingModel.TRUNCKBASED.name())) {
                        jobFlowInstancesList.add(new BuildArtifact(script, configuration))
                    }
                    break
                case 'BuildDockerImage':
                    if (deployOnly) break
                    if (!buildDockerImage) break
                    if ((branchName ==~ /^(dev|develop|hotfix\/.+|release\/.+)$/ && branchingModel == BranchingModel.GITFLOW.name())||(branchName == "master" && branchingModel == BranchingModel.TRUNCKBASED.name())) {
                        jobFlowInstancesList.add(new BuildDockerImage(script, configuration))
                    }
                    break
                case 'PublishArtifact':
                    if (deployOnly) break
                    if (!publishArtifact) break
                    if ((branchName ==~ /^(dev|develop|hotfix\/.+|release\/.+)$/ && branchingModel == BranchingModel.GITFLOW.name())||(branchName == "master" && branchingModel == BranchingModel.TRUNCKBASED.name())) {
                        jobFlowInstancesList.add(new PublishArtifact(script, configuration))
                    }
                    break
                case 'PublishDockerImage':
                    if (deployOnly) break
                    if (!publishDockerImage) break
                    if ((branchName ==~ /^(dev|develop|hotfix\/.+|release\/.+)$/ && branchingModel == BranchingModel.GITFLOW.name())||(branchName == "master" && branchingModel == BranchingModel.TRUNCKBASED.name())) {
                        jobFlowInstancesList.add(new PublishDockerImage(script, configuration))
                    }
                    break
                case 'SecurityScan':
                    if (deployOnly) break
                    if (!isSecurityScanEnabled) break
                    if ((branchName ==~ /^(release|hotfix)\/.+$/ && branchingModel == BranchingModel.GITFLOW.name())||(branchName == "master" && branchingModel == BranchingModel.TRUNCKBASED.name())) {
                        jobFlowInstancesList.add(new SecurityScan(script, configuration))
                    }
                    break
                case 'IntegrationTest':
                    if (deployOnly) break
                    if (branchName ==~ /^(master|dev|develop|hotfix\/.+|release\/.+)$/ && BranchingModel.GITFLOW.name()) break
                    if (branchName == 'master' && branchingModel == BranchingModel.TRUNCKBASED.name()) break
                    jobFlowInstancesList.add(new IntegrationTest(script, configuration))
                    break
                case 'DeployToK8s':
                    if (!deployOnK8s) break
                    if ((branchName ==~ /^(dev|develop|master|release\/.+)$/ && branchingModel == BranchingModel.GITFLOW.name()) || (branchingModel == BranchingModel.TRUNCKBASED.name() && branchName == "master")) {
                        jobFlowInstancesList.add(new DeployToK8s(script, configuration))
                    }
                    break
                case 'DeployByAnsible':
                    if (!ansibleDeployment) break
                    if ((branchName ==~ /^(dev|develop|master|release\/.+)$/ && branchingModel == BranchingModel.GITFLOW.name()) || (branchingModel == BranchingModel.TRUNCKBASED.name() && branchName == "master")) {
                        jobFlowInstancesList.add(new DeployByAnsible(script, configuration))
                    }
                    break
                case 'PostDeploy':
                    if (deployOnly) break
                    if (!postdeploycommands) break
                    if ((branchName ==~ /^(dev|develop|master|release\/.+)$/ && branchingModel == BranchingModel.GITFLOW.name())||(branchName == "master" && branchingModel == BranchingModel.TRUNCKBASED.name())) {
                        jobFlowInstancesList.add(new PostDeploy(script, configuration))
                    }
                    break
                case 'Healthcheck':
                    if((branchName ==~ /^(dev|develop|master|release\/.+)$/ && branchingModel == BranchingModel.GITFLOW.name()) || (branchingModel == BranchingModel.TRUNCKBASED.name() && branchName == "master")) {
                        jobFlowInstancesList.add(new Healthcheck(script, configuration))
                    }
                    break
                case 'QACoreTeamTest':
                    if (deployOnly) break
                    if ((branchName ==~ /^(dev|develop|master|release\/.+)$/ && branchingModel == BranchingModel.GITFLOW.name()) || (branchingModel == BranchingModel.TRUNCKBASED.name() && branchName == "master")) {
                        jobFlowInstancesList.add(new QACoreTeamTest(script, configuration))
                    }
                    break
                // SendNotifications should be moved to the separated method
                case 'SendNotifications':
                    jobFlowInstancesList.add(new SendNotifications(script, configuration))
                    break
                default:
                    throw new IllegalArgumentException("Unknown build step: ${buildStep}")
            }
        }

        return jobFlowInstancesList
    }

    abstract execute()

}
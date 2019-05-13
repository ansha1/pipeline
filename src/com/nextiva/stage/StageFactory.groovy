package com.nextiva.stage


class StageFactory {
    static Map getStages() {
        return Stages
    }
    static final Map Stages = ["Checkout"                    : ["Class"         : Checkout.class,],
                               "VerifyArtifactVersionInNexus": ["deployOnly"    : false,
                                                                "branchingModel": ["gitflow"   : /^((hotfix|release)\/.+)$/,
                                                                                   "trunkbased": /^master$/]
                               ],
                               "UnitTest"                    : ["deployOnly"    : false,
                                                                "Class"         : UnitTest.class,
                                                                "branchingModel": ["gitflow"   : /^!(master)$/,
                                                                                   "trunkbased": /^.*$/]
                               ],
                               "SonarScan"                   : ["deployOnly"            : false,
                                                                "isSonarAnalysisEnabled": true,
                                                                "branchingModel"        : ["gitflow"   : /^(develop|dev)$/,
                                                                                           "trunkbased": /^master$/]
                               ],
                               "IntegrationTest"             : ["deployOnly"               : false,
                                                                "isIntegrationTestsEnabled": true,
                                                                "branchingModel"           : ["gitflow"   : /^!(develop|dev|release\/.+|master)$/,
                                                                                              "trunkbased": /^!(master)$/]
                               ],
                               "BuildArtifact"               : ["deployOnly"    : false,
                                                                "buildArtifact" : true,
                                                                "branchingModel": ["gitflow"   : /^(dev|develop|hotfix\/.+|release\/.+)$/,
                                                                                   "trunkbased": /^master$/]
                               ],
                               "BuildDockerImage"            : ["deployOnly"      : false,
                                                                "buildDockerImage": true,
                                                                "branchingModel"  : ["gitflow"   : /^(dev|develop|hotfix\/.+|release\/.+)$/,
                                                                                     "trunkbased": /^master$/]
                               ],
                               "PublishArtifact"             : ["deployOnly"     : false,
                                                                "publishArtifact": true,
                                                                "branchingModel" : ["gitflow"   : /^(dev|develop|hotfix\/.+|release\/.+)$/,
                                                                                    "trunkbased": /^master$/]
                               ],
                               "PublishDockerImage"          : ["deployOnly"        : false,
                                                                "publishDockerImage": true,
                                                                "branchingModel"    : ["gitflow"   : /^(dev|develop|hotfix\/.+|release\/.+)$/,
                                                                                       "trunkbased": /^master$/],
                               ],
                               "SecurityScan"                : ["deployOnly"           : false,
                                                                "isSecurityScanEnabled": true,
                                                                "branchingModel"       : ["gitflow"   : /^(release|hotfix)\/.+$/,
                                                                                          "trunkbased": /^!(master)$/]
                               ],
                               "Deploy"                      : ["branchingModel": ["gitflow"   : /^(dev|develop|master|release\/.+|hotfix\/.+)$/,
                                                                                   "trunkbased": /^(master)$/]
                               ],
                               "PostDeploy"                  : ["isPostDeployEnabled": true,
                                                                "branchingModel"     : ["gitflow"   : /^(dev|develop|master|release\/.+|hotfix\/.+)$/,
                                                                                        "trunkbased": /^(master)$/]
                               ],
                               "QACoreTeamTest"              : ["branchingModel": ["gitflow"   : /^(dev|develop|master|release\/.+|hotfix\/.+)$/,
                                                                                   "trunkbased": /^(master)$/]],

    ]


    static BasicStage getStage(BasicStage stage, Script script, configuration){
        return  = stage.getDeclaredConstructor(Script, Map).newInstance(this, configuration)
    }

}

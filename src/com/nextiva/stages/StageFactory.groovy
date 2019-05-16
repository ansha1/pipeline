package com.nextiva.stages

import com.nextiva.stages.stage.*

import java.util.regex.Pattern


class StageFactory {

    static final Map stages = ["Checkout"                    : ["class": Checkout.class,],

                               "VerifyArtifactVersionInNexus": ["deployOnly"    : false,
                                                                "branchingModel": ["gitflow"   : /^((hotfix|release)\/.+)$/,
                                                                                   "trunkbased": /^master$/]
                               ],
                               "Build"                       : ["deployOnly"    : false,
                                                                "class"         : BuildArtifact.class,
                                                                "branchingModel": ["gitflow"   : /^(dev|develop|hotfix\/.+|release\/.+)$/,
                                                                                   "trunkbased": /^master$/]
                               ],
                               "UnitTest"                    : ["class"            : UnitTest.class,
                                                                "isUnitTestEnabled": true,
                                                                "deployOnly"       : false,
                                                                "branchingModel"   : ["gitflow"   : /^!(master)$/,
                                                                                      "trunkbased": /^.*$/]
                               ],
                               "SonarScan"                   : ["class"                 : SonarScan.class,
                                                                "deployOnly"            : false,
                                                                "isSonarAnalysisEnabled": true,
                                                                "branchingModel"        : ["gitflow"   : /^(develop|dev)$/,
                                                                                           "trunkbased": /^master$/]
                               ],
                               "IntegrationTest"             : ["deployOnly"              : false,
                                                                "class"                   : IntegrationTest.class,
                                                                "isIntegrationTestEnabled": true,
                                                                "branchingModel"          : ["gitflow"   : /^!(develop|dev|release\/.+|master)$/,
                                                                                             "trunkbased": /^!(master)$/]
                               ],
                               "Publish"                     : ["deployOnly"    : false,
                                                                "class"         : PublishArtifact.class,
                                                                "branchingModel": ["gitflow"   : /^(dev|develop|hotfix\/.+|release\/.+)$/,
                                                                                   "trunkbased": /^master$/]
                               ],
                               "SecurityScan"                : ["deployOnly"           : false,
                                                                "isSecurityScanEnabled": true,
                                                                "class"                : SecurityScan.class,
                                                                "branchingModel"       : ["gitflow"   : /^(release|hotfix)\/.+$/,
                                                                                          "trunkbased": /^!(master)$/]
                               ],
                               "Deploy"                      : ["class"          : Deploy.class,
                                                                "isDeployEnabled": true,
                                                                "branchingModel" : ["gitflow"   : /^(dev|develop|master|release\/.+|hotfix\/.+)$/,
                                                                                    "trunkbased": /^(master)$/]
                               ],
                               "PostDeploy"                  : ["isPostDeployEnabled": true,
                                                                "class"              : PostDeploy.class,
                                                                "branchingModel"     : ["gitflow"   : /^(dev|develop|master|release\/.+|hotfix\/.+)$/,
                                                                                        "trunkbased": /^(master)$/]
                               ],
                               "QACoreTeamTest"              : ["class"                  : QACoreTeamTest.class,
                                                                "isQACoreTeamTestEnabled": true,
                                                                "branchingModel"         : ["gitflow"   : /^(dev|develop|master|release\/.+|hotfix\/.+)$/,
                                                                                            "trunkbased": /^(master)$/]
                               ],
                               "CollectBuildResults"         : ["class": CollectBuildResults.class,
                               ],
                               "SendNotifications"           : ["class": SendNotifications.class,
                               ],
    ]

    static Stage getStage(Script script, Class clazz, Map configuration) {
        try {
            return clazz.getDeclaredConstructor(Script, Map).newInstance(script, configuration)
        } catch (e) {
            script.error("Can't create stage $clazz because of error $e")
        }
    }

    static Stage getStageByName(String stageName, Script script, Map configuration) {
        Class stageClass = stages.get(stageName).get("class")
        return getStage(script, stageClass, configuration)
    }

    static List<Stage> getStagesFromConfiguration(Script script, Map configuration) {
        List<Stage> flow = []
        stages.each { k, v ->
            script.log.debug("get $k and $v and config $configuration")
            if (checkForExecuting(v, configuration)) {
                flow.add(getStage(script, v.get("class"), configuration))
            }
        }
        return flow
    }

    /**
     * checkForExecuting checking that stage should be in the current pipeline flow, based on provided configuration
     */
    static Boolean checkForExecuting(Map stageDefinition, Map configuration) {
        return stageDefinition.every { key, value ->
            switch (key) {
                case "class":
                    return true
                    break
                case "branchingModel":
                    Pattern branchPattern = Pattern.compile(value.get(configuration.get("branchingModel")))
                    return configuration.get("branchName") ==~ branchPattern
                    break
                default:
                    if (configuration.containsKey(key)) {
                        return configuration.get(key) == value
                    } else {
                        return true
                    }
                    break
            }
        }
    }
}
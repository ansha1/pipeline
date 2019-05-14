package com.nextiva.stages

import com.nextiva.stages.stage.BasicStage
import com.nextiva.stages.stage.BuildArtifact
import com.nextiva.stages.stage.BuildDockerImage
import com.nextiva.stages.stage.Checkout
import com.nextiva.stages.stage.Deploy
import com.nextiva.stages.stage.IntegrationTest
import com.nextiva.stages.stage.PostDeploy
import com.nextiva.stages.stage.PublishArtifact
import com.nextiva.stages.stage.PublishDockerImage
import com.nextiva.stages.stage.QACoreTeamTest
import com.nextiva.stages.stage.SecurityScan
import com.nextiva.stages.stage.SonarScan
import com.nextiva.stages.stage.UnitTest

import java.util.regex.Pattern


class StageFactory {

    static final Map stages = ["Checkout"                    : ["class": Checkout.class,],

                               "VerifyArtifactVersionInNexus": ["deployOnly"    : false,
                                                                "branchingModel": ["gitflow"   : /^((hotfix|release)\/.+)$/,
                                                                                   "trunkbased": /^master$/]
                               ],
                               "UnitTest"                    : ["class"         : UnitTest.class,
                                                                "deployOnly"    : false,
                                                                "branchingModel": ["gitflow"   : /^!(master)$/,
                                                                                   "trunkbased": /^.*$/]
                               ],
                               "SonarScan"                   : ["class"                 : SonarScan.class,
                                                                "deployOnly"            : false,
                                                                "isSonarAnalysisEnabled": true,
                                                                "branchingModel"        : ["gitflow"   : /^(develop|dev)$/,
                                                                                           "trunkbased": /^master$/]
                               ],
                               "IntegrationTest"             : ["deployOnly"               : false,
                                                                "class"                    : IntegrationTest.class,
                                                                "isIntegrationTestsEnabled": true,
                                                                "branchingModel"           : ["gitflow"   : /^!(develop|dev|release\/.+|master)$/,
                                                                                              "trunkbased": /^!(master)$/]
                               ],
                               "BuildArtifact"               : ["deployOnly"    : false,
                                                                "class"         : BuildArtifact.class,
                                                                "buildArtifact" : true,
                                                                "branchingModel": ["gitflow"   : /^(dev|develop|hotfix\/.+|release\/.+)$/,
                                                                                   "trunkbased": /^master$/]
                               ],
                               "BuildDockerImage"            : ["deployOnly"      : false,
                                                                "buildDockerImage": true,
                                                                "class"           : BuildDockerImage.class,
                                                                "branchingModel"  : ["gitflow"   : /^(dev|develop|hotfix\/.+|release\/.+)$/,
                                                                                     "trunkbased": /^master$/]
                               ],
                               "PublishArtifact"             : ["deployOnly"     : false,
                                                                "publishArtifact": true,
                                                                "class"          : PublishArtifact.class,
                                                                "branchingModel" : ["gitflow"   : /^(dev|develop|hotfix\/.+|release\/.+)$/,
                                                                                    "trunkbased": /^master$/]
                               ],
                               "PublishDockerImage"          : ["deployOnly"        : false,
                                                                "publishDockerImage": true,
                                                                "class"             : PublishDockerImage.class,
                                                                "branchingModel"    : ["gitflow"   : /^(dev|develop|hotfix\/.+|release\/.+)$/,
                                                                                       "trunkbased": /^master$/],
                               ],
                               "SecurityScan"                : ["deployOnly"           : false,
                                                                "isSecurityScanEnabled": true,
                                                                "class"                : SecurityScan.class,
                                                                "branchingModel"       : ["gitflow"   : /^(release|hotfix)\/.+$/,
                                                                                          "trunkbased": /^!(master)$/]
                               ],
                               "Deploy"                      : ["class"         : Deploy.class,
                                                                "branchingModel": ["gitflow"   : /^(dev|develop|master|release\/.+|hotfix\/.+)$/,
                                                                                   "trunkbased": /^(master)$/]
                               ],
                               "PostDeploy"                  : ["isPostDeployEnabled": true,
                                                                "class"              : PostDeploy.class,
                                                                "branchingModel"     : ["gitflow"   : /^(dev|develop|master|release\/.+|hotfix\/.+)$/,
                                                                                        "trunkbased": /^(master)$/]
                               ],
                               "QACoreTeamTest"              : ["class"         : QACoreTeamTest.class,
                                                                "branchingModel": ["gitflow"   : /^(dev|develop|master|release\/.+|hotfix\/.+)$/,
                                                                                   "trunkbased": /^(master)$/]
                               ],

    ]

    static BasicStage getStage(Script script, Class clazz, Map configuration) {
        try {
            return clazz.getDeclaredConstructor(Script, Map).newInstance(script, configuration)
        } catch (e) {
            script.error("Can't create stage $clazz because of error $e")
        }
    }

    static BasicStage getStageByName(String stageName, Script script, Map configuration) {
        Class stageClass = stages.get(stageName).get("class")
        return getStage(script, stageClass, configuration)
    }

    static List<BasicStage> getStagesFromConfiguration(Script script, Map configuration) {
        List<BasicStage> flow = []
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
                    return configuration.get("branch") ==~ branchPattern
                    break
                default:
                    if (configuration.containsKey(key)) {
                        return configuration.get(key).equals(value)
                    } else {
                        return true
                    }
                    break
            }
        }
    }
}
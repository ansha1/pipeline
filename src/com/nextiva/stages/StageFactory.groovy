package com.nextiva.stages

import com.nextiva.stages.stage.*
import com.nextiva.utils.Logger

import java.util.regex.Pattern


class StageFactory {
    Script script
    Map configuration
    Logger log = new Logger(this)

    StageFactory(Script script, Map configuration) {
        this.script = script
        this.configuration = configuration
    }
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
                                                                "branchingModel"   : ["gitflow"   : /^(?!master)$/,
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
                                                                                             "trunkbased": /^(?!master)$/]
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
                                                                                          "trunkbased": /^(?!master)$/]
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

    Stage createStage(Class clazz) {
        try {
            return clazz.getDeclaredConstructor(Script, Map).newInstance(script, configuration)
        } catch (e) {
            script.error("Can't create stage $clazz because of error $e")
        }
    }

    Stage getStageByName(String stageName) {
        Class stageClass = stages.get(stageName).get("class")
        return createStage(stageClass)
    }

    List<Stage> getStagesFromConfiguration() {
        log.debug("Check what steps are allowed to perform with this configuration")
        List<Stage> flow = []
        stages.each { k, v ->
            log.debug("Trying to create stage: $k  with definition: \n $v \n")
            Boolean executing = checkForExecuting(v, configuration)
            log.debug("Stage $k add: $executing")
            if (executing) {
                flow.add(createStage(v.get("class")))
            }
        }
        log.debug("======================================================================================")
        log.debug("Current flow: ", flow)
        return flow
    }

    /**
     * checkForExecuting checking that stage should be in the current pipeline flow, based on provided configuration
     */
    Boolean checkForExecuting(Map stageDefinition, Map configuration) {
        Boolean executing = false

        executing = stageDefinition.every { key, value ->
            log.debug("Checking key $key")
            switch (key) {
                case "class":
                    return true
                    break
                case "branchingModel":
                    String branchingModel = configuration.get("branchingModel")
                    Pattern branchPattern = Pattern.compile(value.get(branchingModel))
                    String branchName = configuration.get("branchName")
                    log.debug("Branching model from configuration: $branchingModel")
                    log.debug("Comparing branch pattern $branchPattern with branch $branchName")
                    Boolean result = branchingModel ==~ branchPattern
                    log.debug("result: $result")
                    return result
                    break
                default:
                    if (configuration.containsKey(key)) {
                        def configurationValue = configuration.get(key)
                        log.debug("comparing configuration value: $configurationValue and definition value: $value for this key: $key")
                        Boolean result = configurationValue == value
                        log.debug("result: $result")
                        return result
                    } else {
                        return true
                    }
                    break
            }
        }
        return executing
    }
}
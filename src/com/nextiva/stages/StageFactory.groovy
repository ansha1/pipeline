package com.nextiva.stages

import com.nextiva.stages.stage.*
import com.nextiva.utils.Logger

import java.util.regex.Pattern

import static com.nextiva.config.Global.instance as global


class StageFactory {
    Script script
    Map configuration
    Logger logger = new Logger(this)

    StageFactory(Script script, Map configuration) {
        this.script = script
        this.configuration = configuration
    }

    private static final def branchingModelRegexps = [
            any               : /.+/,
            notMaster         : /^(?!master$).+/,
            releaseOrHotfix   : /^((hotfix|release)\/.+)/,
            master            : /^master$/,
            mainline          : /^((dev|develop)$|((hotfix|release)\/.+))/,
            mainlineWithMaster: /^((dev|develop|master)$|(hotfix|release)\/.+)/,
            notMainline       : /^(?!(dev|develop|master)$|(hotfix|release)\/).+/,
            develop           : /^(develop|dev)$/
    ]

    static final Map stages = [
            "Checkout"                    : ["class": Checkout.class,
            ],
            "StartBuildDependencies"      : ["deployOnly"          : false,
                                             "isJobHasDependencies": true,
                                             "class"               : StartBuildDependencies.class,
                                             "branchingModel"      : ["gitflow"   : branchingModelRegexps.notMaster,
                                                                      "trunkbased": branchingModelRegexps.any]
            ],
            "VerifyArtifactVersionInNexus": ["deployOnly"    : false,
                                             "class"         : VerifyArtifactVersionInNexus.class,
                                             "branchingModel": ["gitflow"   : branchingModelRegexps.releaseOrHotfix,
                                                                "trunkbased": branchingModelRegexps.master]
            ],
            "ConfigureProjectVersion"     : ["deployOnly"    : false,
                                             "class"         : ConfigureProjectVersion.class,
                                             "branchingModel": ["gitflow"   : branchingModelRegexps.any,
                                                                "trunkbased": branchingModelRegexps.any]
            ],
            "Build"                       : ["deployOnly"    : false,
                                             "class"         : Build.class,
                                             "branchingModel": ["gitflow"   : branchingModelRegexps.notMaster,
                                                                "trunkbased": branchingModelRegexps.any]
            ],
            "UnitTest"                    : ["class"            : UnitTest.class,
                                             "isUnitTestEnabled": true,
                                             "deployOnly"       : false,
                                             "branchingModel"   : ["gitflow"   : branchingModelRegexps.notMaster,
                                                                   "trunkbased": branchingModelRegexps.any]
            ],
            "SonarScan"                   : ["class"                 : SonarScan.class,
                                             "deployOnly"            : false,
                                             "isSonarAnalysisEnabled": true,
                                             "branchingModel"        : ["gitflow"   : branchingModelRegexps.develop,
                                                                        "trunkbased": branchingModelRegexps.master]
            ],
            "IntegrationTest"             : ["deployOnly"              : false,
                                             "class"                   : IntegrationTest.class,
                                             "isIntegrationTestEnabled": true,
                                             "branchingModel"          : ["gitflow"   : branchingModelRegexps.any,
                                                                          "trunkbased": branchingModelRegexps.any]
            ],
            "Publish"                     : ["deployOnly"    : false,
                                             "class"         : Publish.class,
                                             "branchingModel": ["gitflow"   : branchingModelRegexps.mainline,
                                                                "trunkbased": branchingModelRegexps.master]
            ],
            "SecurityScan"                : ["deployOnly"           : false,
                                             "isSecurityScanEnabled": true,
                                             "class"                : SecurityScan.class,
                                             "branchingModel"       : ["gitflow"   : branchingModelRegexps.releaseOrHotfix,
                                                                       "trunkbased": branchingModelRegexps.notMaster]
            ],
            "Deploy"                      : ["class"          : Deploy.class,
                                             "isDeployEnabled": true,
                                             "branchingModel" : ["gitflow"   : branchingModelRegexps.mainlineWithMaster,
                                                                 "trunkbased": branchingModelRegexps.master]
            ],
            "QACoreTeamTest"              : ["class"                  : QACoreTeamTest.class,
                                             "isQACoreTeamTestEnabled": true,
                                             "isDeployEnabled": true,
                                             "branchingModel"         : ["gitflow"   : branchingModelRegexps.mainlineWithMaster,
                                                                         "trunkbased": branchingModelRegexps.master]
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
        logger.debug("Check what steps are allowed to perform with this configuration")
        List<Stage> flow = []
        stages.each { k, v ->
            logger.debug("Trying to create stage: $k  with definition: \n $v \n")
            Boolean executing = checkForExecuting(v, configuration)
            logger.debug("Stage $k add: $executing")
            if (executing) {
                flow.add(createStage(v.get("class")))
            }
        }
        logger.debug("======================================================================================")
        logger.debug("Current flow: $flow ")
        return flow
    }

    /**
     * checkForExecuting checking that stage should be in the current pipeline flow, based on provided configuration
     */
    Boolean checkForExecuting(Map stageDefinition, Map configuration) {
        Boolean executing = false

        executing = stageDefinition.every { key, value ->
            logger.debug("Checking key $key")
            switch (key) {
                case "class":
                    logger.debug("Key 'class' is not a subject for a check. Skipping...")
                    return true
                    break
                case "branchingModel":
                    String branchingModel = configuration.get("branchingModel")
                    Pattern branchPattern = Pattern.compile(value.get(branchingModel))
                    String branchName = configuration.get("branchName")
                    logger.debug("Branching model from configuration: $branchingModel")
                    logger.debug("Comparing branch pattern $branchPattern with branch $branchName")
                    Boolean result = branchName ==~ branchPattern
                    logger.debug("result: $result")
                    return result
                    break
                default:
                    if (global.properties.containsKey(key)) {
                        def configurationValue = global.properties.get(key)
                        logger.debug("comparing configuration value: $configurationValue and definition value: $value for this key: $key")
                        Boolean result = configurationValue == value
                        logger.debug("result: $result")
                        return result
                    }
                    // TODO remove below if clause when all configuration will be moved into Global singleton
                    if (configuration.containsKey(key)) {
                        def configurationValue = configuration.get(key)
                        logger.debug("comparing configuration value: $configurationValue and definition value: $value for this key: $key")
                        Boolean result = configurationValue == value
                        logger.debug("result: $result")
                        return result
                    }
                    return true
                    break
            }
        }
        return executing
    }
}
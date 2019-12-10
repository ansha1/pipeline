package com.nextiva.stages

import com.nextiva.config.BranchingModelRegexps
import com.nextiva.config.GitFlow
import com.nextiva.config.TrunkBased
import com.nextiva.stages.stage.*
import com.nextiva.utils.Logger

import static com.nextiva.config.Config.instance as config


class StageFactory {
    Logger logger = new Logger(this)

    StageFactory() {}

    static final Map<Class, Map> stages = [
            (Checkout.class)                    : [:],

            (StartBuildDependencies.class)      : [
                    "deployOnly"          : false,
                    "isJobHasDependencies": true,
                    "branchingModel"      : [(GitFlow.class)   : BranchingModelRegexps.notMaster,
                                             (TrunkBased.class): BranchingModelRegexps.any]
            ],
            (VerifyArtifactVersionInNexus.class): [
                    "deployOnly"    : false,
                    "branchingModel": [(GitFlow.class)   : BranchingModelRegexps.releaseOrHotfix,
                                       (TrunkBased.class): BranchingModelRegexps.master]
            ],
            (ConfigureProjectVersion.class)     : [
                    "deployOnly"    : false,
                    "version"       : null
            ],
            (Build.class)                       : [
                    "deployOnly"    : false,
                    "branchingModel": [(GitFlow.class)   : BranchingModelRegexps.notMaster,
                                       (TrunkBased.class): BranchingModelRegexps.any]
            ],
            (UnitTest.class)                    : [
                    "isUnitTestEnabled": true,
                    "deployOnly"       : false,
                    "branchingModel"   : [(GitFlow.class)   : BranchingModelRegexps.notMaster,
                                          (TrunkBased.class): BranchingModelRegexps.any]
            ],
            (SonarScan.class)                   : [
                    "deployOnly"            : false,
                    "isSonarAnalysisEnabled": true,
                    "branchingModel"        : [(GitFlow.class)   : BranchingModelRegexps.develop,
                                               (TrunkBased.class): BranchingModelRegexps.master]
            ],
            (IntegrationTest.class)             : [
                    "deployOnly"              : false,
                    "isIntegrationTestEnabled": true,
                    "branchingModel"          : [(GitFlow.class)   : BranchingModelRegexps.notMaster,
                                                 (TrunkBased.class): BranchingModelRegexps.any]
            ],
            (Publish.class)                     : [
                    "deployOnly"    : false,
                    "branchingModel": [(GitFlow.class)   : BranchingModelRegexps.mainline,
                                       (TrunkBased.class): BranchingModelRegexps.master]
            ],
            (SecurityScan.class)                : [
                    "deployOnly"           : false,
                    "isSecurityScanEnabled": true,
                    "branchingModel"       : [(GitFlow.class)   : BranchingModelRegexps.releaseOrHotfix,
                                              (TrunkBased.class): BranchingModelRegexps.master]
            ],
            (Deploy.class)                      : ["isDeployEnabled": true,],
            (QACoreTeamTest.class)              : [
                    "isQACoreTeamTestEnabled": true,
                    "isDeployEnabled"        : true,
                    "branchingModel"         : [(GitFlow.class)   : BranchingModelRegexps.mainlineWithMaster,
                                                (TrunkBased.class): BranchingModelRegexps.master]
            ],
            (CollectBuildResults.class)         : [:],
            (SendNotifications.class)           : [:],
    ]

    Stage createStage(Class clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance()
        } catch (e) {
            config.script.error("Can't create stage $clazz because of error $e")
        }
    }

    List<Stage> getStagesFromConfiguration() {
        logger.debug("Check what steps are allowed to perform with this configuration")
        List<Stage> flow = []

        stages.each { Class stageClass, Map stageDefinition ->
            logger.debug("Trying to create stage: ${stageClass.simpleName}  with definition: \n $stageDefinition \n")
            Boolean executing = checkForExecuting(stageDefinition)
            logger.debug("Stage ${stageClass.simpleName} add: $executing")
            if (executing) {
                flow.add(createStage(stageClass))
            }
        }
        logger.debug("======================================================================================")
        logger.debug("Current flow: ${flow.collect { it.class.simpleName }}")
        return flow
    }

    /**
     * checkForExecuting checking that stage should be in the current pipeline flow, based on provided configuration
     */
    Boolean checkForExecuting(Map stageDefinition) {
        Boolean executing = false

        executing = stageDefinition.every { key, value ->
            logger.debug("Checking key $key")
            switch (key) {
                case "class":
                    logger.trace("Key 'class' is not a subject for a check. Skipping...")
                    return true
                    break
                case "branchingModel":
                    def branchPattern = value.get(config.branchingModel.class)
                    logger.debug("Comparing branch pattern $branchPattern with branch $config.branchName")
                    Boolean result = config.branchName ==~ branchPattern
                    logger.debug("result: $result")
                    return result
                    break
                default:
                    if (config.properties.containsKey(key)) {
                        def configurationValue = config.properties.get(key)
                        logger.debug("Comparing configuration value: $configurationValue and definition value: $value")
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
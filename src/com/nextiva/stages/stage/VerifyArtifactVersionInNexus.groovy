package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool
import static com.nextiva.config.Config.instance as config


class VerifyArtifactVersionInNexus extends Stage {
    VerifyArtifactVersionInNexus() {
        super()
    }

    def stageBody() {
        List build = config.build

        build.each { toolConfiguration ->
            withStage("${toolConfiguration.name} ${stageName}") {
                BuildTool tool = toolConfiguration.get("instance")
                try {
                    logger.debug("checking artifact availability for ${toolConfiguration.name}")
                    if (tool.isArtifactAvailableInRepo()) {
                        logger.info("The artifact already exists in Nexus")
                        throw new Exception("The artifact with version ${config.version} already exists in Nexus")
//                        TODO: add jenkins approve step for autoincrement
//                        try {
//                            timeout(time: 15, unit: 'MINUTES') {
//                                bot.getJenkinsApprove("@${common.getCurrentUserSlackId()}", "Approve", "Decline",
//                                        "Increase a patch version for ${jobConfig.APP_NAME}", "${BUILD_URL}input/",
//                                        "Package ${jobConfig.APP_NAME} with version ${jobConfig.BUILD_VERSION} " +
//                                                "already exists in Nexus. \n" +
//                                                "Do you want to increase a patch version and continue the process?"
//                                        , "${BUILD_URL}input/", jobConfig.branchPermissions)
//                            }
//                        } catch (e) {
//                            currentBuild.rawBuild.result = Result.ABORTED
//                            throw new hudson.AbortException("Aborted")
//                        }
//                    def patchedBuildVersion = jobConfig.autoIncrementVersion(jobConfig.semanticVersion.bumpPatch())
//                    utils.setVersion(jobConfig.version)
//
//                    sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
//                        sh """
//                                            git config --global user.name "Nextiva Jenkins"
//                                            git config --global user.email "jenkins@nextiva.com"
//                                            git commit -a -m "Auto increment of $jobConfig.BUILD_VERSION - bumped to $patchedBuildVersion"
//                                            git push origin HEAD:${BRANCH_NAME}
//                                        """
//                    }

                    } else {
                        logger.info("Current artifact version does not exist in Nexus, continue...")
                    }
                } catch (e) {
                    logger.error("Error when executing ${tool.name} ${stageName}:", e)
                    throw e
                }
            }
        }
    }


//    def autoIncrementVersion(SemanticVersion currentVersion) {
//        semanticVersion = currentVersion
//        version = semanticVersion.toString()
//        patchedBuildVersion = currentVersion.toString()
//
//        if (utils.verifyPackageInNexus(APP_NAME, patchedBuildVersion, DEPLOY_ENVIRONMENT)) {
//            patchedBuildVersion = autoIncrementVersion(semanticVersion.bump(PatchLevel.PATCH))
//        }
//
//        return patchedBuildVersion
//    }
}
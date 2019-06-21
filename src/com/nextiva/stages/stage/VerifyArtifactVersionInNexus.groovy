package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool


class VerifyArtifactVersionInNexus extends Stage {
    VerifyArtifactVersionInNexus(Script script, Map configuration) {
        super(script, configuration)
    }

    def stageBody() {
        Map build = configuration.get("build")

        build.each { toolName, toolConfig ->
            withStage("${toolName} ${stageName()}") {
                BuildTool tool = toolConfig.get("instance")
                try {
                    log.debug("checking artifact availability for ${toolName}")
                    if (tool.isArtifactAvailableInRepo()) {
                        log.info("the artifact is already exist")
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
                        log.info("Current artifact version is not exist in the Nexus, continue...")
                    }
                } catch (e) {
                    log.error("Error when executing ${toolName} ${stageName()}:", e)
                    throw e
                }
            }
        }
    }
}
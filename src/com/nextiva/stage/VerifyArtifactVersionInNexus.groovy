package com.nextiva.stage

class VerifyArtifactVersionInNexus extends BasicStage {
    VerifyArtifactVersionInNexus(script, configuration) {
        super(script, configuration)
    }

    def execute() {
        script.stage(this.getClass().getSimpleName()) {
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
//
//            if (configuration.DEPLOY_ONLY == false && env.BRANCH_NAME ==~ /^((hotfix|release)\/.+)$/) {
//
//
//                if (utils.verifyPackageInNexus(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.DEPLOY_ENVIRONMENT)) {
//
//                    // Old implementation with non-interactive notification
//                    // It's here to quickly switch to it if jenkins bot doesn't work.
//                    /*
//                        approve.sendToPrivate("Package ${jobConfig.APP_NAME} with version ${jobConfig.BUILD_VERSION} " +
//                                "already exists in Nexus. " +
//                                "Do you want to increase a patch version and continue the process?",
//                                common.getCurrentUserSlackId(), jobConfig.branchPermissions)
//                         */
//
//                    try {
//                        timeout(time: 15, unit: 'MINUTES') {
//                            bot.getJenkinsApprove("@${common.getCurrentUserSlackId()}", "Approve", "Decline",
//                                    "Increase a patch version for ${jobConfig.APP_NAME}", "${BUILD_URL}input/",
//                                    "Package ${jobConfig.APP_NAME} with version ${jobConfig.BUILD_VERSION} " +
//                                            "already exists in Nexus. \n" +
//                                            "Do you want to increase a patch version and continue the process?"
//                                    , "${BUILD_URL}input/", jobConfig.branchPermissions)
//                        }
//                    } catch (e) {
//                        currentBuild.rawBuild.result = Result.ABORTED
//                        throw new hudson.AbortException("Aborted")
//                    }
//
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
//
//                    jobConfig.setBuildVersion()
//                }
//            }

        }
    }
}
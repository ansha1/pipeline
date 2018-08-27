#!groovy
/*
This method allows to merge two branches and resolve conflicts automatically, if they affect version in package.json | pom.xml | build.properties only
if it`s true this method accepts the destination branch changes
*/

import static com.nextiva.SharedJobsStaticVars.*

def call(String sourceBranch, String destinationBranch, String channelToNotify, Boolean autoPullRequest = false, Boolean autoMerge = false) {
    log.info("Prepare to merge ${sourceBranch} -> ${destinationBranch}")
    log.info("channelToNotify: ${channelToNotify}")
    log.info("autoPullRequest: ${autoPullRequest} ")
    log.info("autoMerge: ${autoMerge}")
    sourceBranch = sourceBranch.replaceAll("origin/", "")
    destinationBranch = destinationBranch.replaceAll("origin/", "")

    log.info("Starting soft merge")
    if (softMerge(sourceBranch, destinationBranch)) {
        log.info("Merged ${sourceBranch} to ${destinationBranch}")
    } else if (autoMerge && isMergeable()) {
        forceMerge(sourceBranch, destinationBranch)
    } else if (autoPullRequest) {
        log.info("AUTO CREATING PULL REQUEST IS ENABLED  autoPullRequest=${autoPullRequest}")
        pullRequest(sourceBranch, destinationBranch, channelToNotify)
        error("\n\nCan`t merge ${sourceBranch} to ${destinationBranch} \n You need to resolve merge conflicts manually with pull request\n\n")
    } else {
        error("\n\nCan`t merge ${sourceBranch} to ${destinationBranch} \n You need to resolve merge conflicts manually\n\n")
    }
}

/*
This method assert that all conflicts only in versions
 */

Boolean isMergeable() {
    def isMergeable = true
    def allowedConflictFilesPattern = /((.*\/)?pom.xml|package(-lock)?.json|build.properties)/
    def allowedConflictLinesPattern = /(<version>.*<\/version>|"version": ".*",|version=.*)/

    log.info("Ensure that merge conflicts only in version files")
    def filesWithConflictsRaw = sh returnStdout: true, script: "git ls-files -u | awk '{print \$4}' | uniq"
    log.info("FOUND CONFLICT IN FILES: \n${filesWithConflictsRaw}\n =========================")
    def filesWithConflicts = filesWithConflictsRaw.trim().split("\n") as Set

    for (file in filesWithConflicts) {
        if (file.trim() ==~ allowedConflictFilesPattern) {
            log.info("\u2705 Changes in ${file} can be automerged... \n looking for conflicts inside")
            def conflictLinesRaw = sh returnStdout: true, script: "pcregrep --buffer-size=1024 -Mi '<<<(.|\\n)*\\n>>>' ${file.trim()} | grep -vE '(>>|==|<<)'|sed '/^\$/d'"
            log.info("FOUND CONFLICT IN LINES: \n ${conflictLinesRaw} \n =======================")
            def conflictLines = conflictLinesRaw.trim().split("\n") as Set

            for (line in conflictLines) {
                if (line.trim() ==~ allowedConflictLinesPattern) {
                    log.info("\u2705 This conflict ${line.trim()} is OK and can be automerged")
                } else {
                    log.error("\u274E Conflicts in line: ${line.trim()}\n in file ${file.trim()} can`t be resolved automatically")
                    isMergeable = false
                    break
                }
            }
        } else {
            log.error("\u274E Conflicts in file ${file} can`t be resolved automatically")
            isMergeable = false
            break
        }
    }
    return isMergeable
}

Boolean softMerge(String sourceBranch, String destinationBranch) {

    log.info("Trying to merge ${sourceBranch} to ${destinationBranch}")
    try {
        sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
            sh """
                git fetch
                git checkout ${sourceBranch}
                git checkout ${destinationBranch}
                git merge --no-ff ${sourceBranch}
            """
        }
        return true
    } catch (e) {
        log.warn("soft merge failed: ${e}")
        return false
    }
}

void forceMerge(String sourceBranch, String destinationBranch) {
    log.info("All merge conflicts are in version, so we can accept ${destinationBranch} changes")
    sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
        sh """
            git reset --merge
            git checkout ${destinationBranch}
            git merge --no-ff -s recursive -Xours ${sourceBranch}
        """
    }
}

def pullRequest(String sourceBranch, String destinationBranch, String channelToNotify) {

    def tmpBranch = "resolve-conflicts-from-${sourceBranch}-to-${destinationBranch}"
    sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
        sh """
            git reset --merge           
            git checkout ${sourceBranch}
            git checkout -b ${tmpBranch} ${sourceBranch}
            git push origin ${tmpBranch}
        """
    }

    stage("Create pull request from ${sourceBranch} to ${destinationBranch}")
    String jobName = URLDecoder.decode(env.JOB_NAME.toString(), 'UTF-8')
    def title = "DO NOT SQUASH THIS PR. Resolve merge conflicts ${sourceBranch}->${destinationBranch}"
    def description = "DO NOT SELECT SQUASH OPTION WHEN MERGING THIS PR (if its enabled for the repository), otherwise there will be conflicts when merging because missing commit history. Auto created pull request from ${jobName} ${env.BUILD_ID}"
    def repositoryUrl = sh returnStdout: true, script: "git config --get remote.origin.url"
    repositoryUrl = repositoryUrl.trim()
    pullRequestLink = bitbucket.createPr(repositoryUrl, tmpBranch, destinationBranch, title, description)

    def uploadSpec = """[{
                        "title": "Failed to automatically merge ${sourceBranch} into ${destinationBranch}.",
                        "text": "Resolve conflicts and merge pull request",
                        "color": "#73797a",
                        "attachment_type": "default",
                        "actions": [{
                                "text": "Link on pull request",
                                "type": "button",
                                "url": "${pullRequestLink}"
                            }]
                    }]"""
    slack(channelToNotify, uploadSpec)
}
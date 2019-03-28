package com.nextiva.slack

import com.google.common.collect.ImmutableList
import com.nextiva.slack.dto.SlackMessage
import com.nextiva.slack.dto.blocks.Block
import com.nextiva.slack.dto.blocks.Section
import com.nextiva.slack.dto.composition.Text
import com.nextiva.slack.dto.blocks.Divider
import hudson.tasks.test.AbstractTestResultAction

class MessagesFactory implements Serializable {

    static def buildStatusMessage(context) {
        List<Block> blocks = new ArrayList<>()

        blocks.add(new Divider())

        Section title = new Section()
        title.setText(new Text(getBuildInfoTitle(context)))
        blocks.add(title)

        Section infoBlocks = new Section()
        infoBlocks.setFields(ImmutableList.of(
                new Text(getBuildBranch()),
                new Text(getStatus()),
                new Text(getTestResults()))
        )
        blocks.add(infoBlocks)

        def message = new SlackMessage()
        message.setBlocks(blocks)

        return message
    }

    private static getBuildInfoTitle(context) {
        def mention = ''
        def buildStatus = context.currentBuild.currentResult
        if (buildStatus ==~ "FAILURE" && context.env.BRANCH_NAME ==~ /^(release\/.+|dev|master)$/) {
            mention = "@here "
        }
        String jobName = URLDecoder.decode(context.env.JOB_NAME.toString(), 'UTF-8')
        def subject = "Job: ${jobName}, build #${context.env.BUILD_NUMBER}"
        return "${mention}*${subject}*"
    }

    private static getBuildBranch(context) {
        return "*Branch:* ${context.env.BRANCH_NAME}"
    }

    private static getStatus(context) {
        return "*Status:* ${context.currentBuild.currentResult}"
    }

    private static getTestResults(context) {
        def testResultAction = context.currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
        def summary

        if (testResultAction != null) {
            def total = testResultAction.getTotalCount()
            def failed = testResultAction.getFailCount()
            def skipped = testResultAction.getSkipCount()

            summary = "Passed: " + (total - failed - skipped)
            summary = summary + (", Failed: " + failed)
            summary = summary + (", Skipped: " + skipped)
        } else {
            summary = "No tests found"
        }
        return "*Test results:* ${summary}"
    }

    private static getCommitAuthor() {
        def commit = sh(returnStdout: true, script: 'git rev-parse HEAD')
        return sh(returnStdout: true, script: "git --no-pager show -s --format='%an' ${commit}").trim()
    }

    private static getLastCommitMessage() {
        return sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
    }

}



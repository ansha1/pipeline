package com.nextiva.slack

import com.google.common.collect.ImmutableList
import com.nextiva.slack.dto.SlackMessage
import com.nextiva.slack.dto.blocks.Block
import com.nextiva.slack.dto.blocks.Context
import com.nextiva.slack.dto.blocks.Section
import com.nextiva.slack.dto.composition.Text
import hudson.tasks.test.AbstractTestResultAction

class MessagesFactory implements Serializable {
    private final def context

    MessagesFactory(context) {
        this.context = context
    }

    def buildStatusMessage() {
        List<Block> blocks = new ArrayList<>()

        Section title = new Section()
        title.setText(new Text(getBuildInfoTitle()))
        blocks.add(title)

        Section infoBlocks = new Section()
        infoBlocks.setFields(ImmutableList.of(
                new Text(getBuildBranch()),
                new Text(getStatus()),
                new Text(getTestResults()))
        )
        blocks.add(infoBlocks)

        Context commitAuthor = new Context()
        commitAuthor.setElements(ImmutableList.of(new Text(getCommitAuthor())))
        blocks.add(commitAuthor)

        Section lastCommitMessage = new Section()
        lastCommitMessage.setText(new Text(getLastCommitMessage()))
        blocks.add(lastCommitMessage)

        def message = new SlackMessage()
        message.setBlocks(blocks)

        return message
    }

    private getBuildInfoTitle() {
        def mention = ''
        def buildStatus = context.currentBuild.currentResult
        if (buildStatus ==~ "FAILURE" && context.env.BRANCH_NAME ==~ /^(release\/.+|dev|master)$/) {
            mention = "@here "
        }
        String jobName = URLDecoder.decode(context.env.JOB_NAME.toString(), 'UTF-8')
        def subject = "Job: ${jobName}, build #${context.env.BUILD_NUMBER}"
        return "${mention}*${subject}*"
    }

    private getBuildBranch() {
        return "*Branch:* ${context.env.BRANCH_NAME}"
    }

    private getStatus() {
        return "*Status:* ${context.currentBuild.currentResult}"
    }

    private getTestResults() {
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

    private getCommitAuthor() {
        def commit = context.sh(returnStdout: true, script: 'git rev-parse HEAD')
        def commitAuthor = context.sh(returnStdout: true, script: "git --no-pager show -s --format='%an' ${commit}").trim()
        return "Commit author: ${commitAuthor}"
    }

    private getLastCommitMessage() {
        def lastCommitMessage = context.sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
        return "'''${lastCommitMessage}'''"
    }

}



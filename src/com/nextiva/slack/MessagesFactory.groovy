package com.nextiva.slack

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.nextiva.slack.dto.SlackMessage
import com.nextiva.slack.dto.attachments.Attachment
import com.nextiva.slack.dto.blocks.Actions
import com.nextiva.slack.dto.blocks.Block
import com.nextiva.slack.dto.blocks.Context
import com.nextiva.slack.dto.blocks.Divider
import com.nextiva.slack.dto.blocks.Section
import com.nextiva.slack.dto.composition.Text
import com.nextiva.slack.dto.interactive.LinkButton
import hudson.tasks.test.AbstractTestResultAction

import static com.nextiva.SharedJobsStaticVars.*

class MessagesFactory implements Serializable {
    private final def context

    MessagesFactory(context) {
        this.context = context
    }

    def buildStatusMessage(errorMessage = '') {
        List<Block> infoBlocks = new ArrayList<>()

        Section title = new Section()
        title.setText(new Text(createBuildInfoTitle()))
        infoBlocks.add(title)

        if (!errorMessage.isEmpty()) {
            infoBlocks.add(new Divider())

            Section error = new Section()
            error.setText(new Text(createErrorMessage(errorMessage)))
            infoBlocks.add(error)
        }

        infoBlocks.add(new Divider())

        Section infoFields = new Section()
        infoFields.setFields(ImmutableList.of(
                new Text(createStatus()),
                new Text(createBuildBranch()),
                new Text(createTestResults()))
        )
        infoBlocks.add(infoFields)

        infoBlocks.add(new Divider())

        Section lastCommitMessage = new Section()
        lastCommitMessage.setText(new Text(createLastCommitMessage()))
        infoBlocks.add(lastCommitMessage)

        Context commitAuthor = new Context()
        commitAuthor.setElements(ImmutableList.of(new Text(createCommitAuthor())))
        infoBlocks.add(commitAuthor)

        List<Block> actionsBlocks = new ArrayList<>()

        Actions buttons = new Actions()
        buttons.setElements(Lists.newArrayList(createJobLinkButton(), createJobConsoleButton()))
        if (hasTestResults()) {
            buttons.getElements().add(createTestResultsButton())
        }
        actionsBlocks.add(buttons)

        Attachment infoAttachment = new Attachment()
        infoAttachment.setBlocks(infoBlocks)
        infoAttachment.setColor("${SLACK_NOTIFY_COLORS.get(context.currentBuild.currentResult)}")

        Attachment actionsAttachment = new Attachment()
        infoAttachment.setBlocks(actionsBlocks)
        infoAttachment.setColor("${SLACK_NOTIFY_COLORS.get(context.currentBuild.currentResult)}")

        def message = new SlackMessage()
        message.setAttachments(ImmutableList.of(infoAttachment, actionsAttachment))

        return message
    }

    def buildApproveMessage(title) {
        List<Block> blocks = new ArrayList<>()

        Section titleSection = new Section()
        titleSection.setText(new Text(title))
        blocks.add(titleSection)

        Section mainText = new Section()
        mainText.setText(new Text(createApproveText()))
        blocks.add(mainText)

        Actions buttons = new Actions()
        buttons.setElements(ImmutableList.of(createApproveButton()))
        blocks.add(buttons)

        Attachment attachment = new Attachment()
        attachment.setBlocks(blocks)
        attachment.setColor("#022ef2")

        def message = new SlackMessage()
        message.setAttachments(ImmutableList.of(attachment))

        return message
    }

    def buildDeployStartMessage(String appName, String version, String environment, String triggeredBy) {
        List<Block> blocks = new ArrayList<>()

        Section mainText = new Section()
        mainText.setText(new Text(createDeployStartText(appName, version, environment, triggeredBy)))
        blocks.add(mainText)

        def message = new SlackMessage()
        message.setBlocks(blocks)

        return message
    }

    def buildDeployFinishMessage(String appName, String version, String environment) {
        List<Block> blocks = new ArrayList<>()

        Section mainText = new Section()
        mainText.setText(new Text(createDeployFinishText(appName, version, environment)))
        blocks.add(mainText)

        def message = new SlackMessage()
        message.setBlocks(blocks)

        return message
    }

    private createDeployFinishText(String appName, String version, String environment) {
        def text = "`${appName}:${version}` ${environment.toUpperCase()} "
        if (context.currentBuild.currentResult == "SUCCESS") {
            List wishList = context.libraryResource('wishes.txt').readLines()
            def randomWish = wishList[new Random().nextInt(wishList.size())]
            text += "deployed :tada: ${randomWish}"
        } else {
            text += "deploy failed! :disappointed:"
        }
        return text
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private createDeployStartText(String appName, String version, String environment, String triggeredBy) {
        return "`${appName}:${version}` ${environment.toUpperCase()} deploy started by <@${triggeredBy}>"
    }

    private createApproveText() {
        return "Stage ${context.env.STAGE_NAME} in ${getJobName()} is waiting for your approval"
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private createErrorMessage(errorMessage) {
        return "*Error:* ${errorMessage}"
    }

    private createBuildInfoTitle() {
        def mention = ''
        def buildStatus = context.currentBuild.currentResult
        if (buildStatus ==~ "FAILURE" && context.env.BRANCH_NAME ==~ /^(release\/.+|dev|master)$/) {
            mention = "@here "
        }
        def subject = "Job: ${getJobName()}, build #${context.env.BUILD_NUMBER}"
        return "${mention}*${subject}*"
    }

    private getJobName() {
        return URLDecoder.decode(context.env.JOB_NAME.toString(), 'UTF-8')
    }

    private createBuildBranch() {
        return "*Branch:* \n${context.env.BRANCH_NAME}"
    }

    private createStatus() {
        return "*Status:* \n`${context.currentBuild.currentResult}`"
    }

    private createTestResults() {
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
        return "*Test results:* \n${summary}"
    }

    private hasTestResults() {
        return context.currentBuild.rawBuild.getAction(AbstractTestResultAction.class) != null
    }

    private createCommitAuthor() {
        def commitAuthor
        try {
            def commit = context.sh(returnStdout: true, script: 'git rev-parse HEAD')
            commitAuthor = context.sh(returnStdout: true, script: "git --no-pager show -s --format='%an' ${commit}").trim()
        } catch (ignored) {
            commitAuthor = "Unknown"
        }
        return "Commit author: ${commitAuthor}"
    }

    private createLastCommitMessage() {
        def lastCommitMessage
        try {
            lastCommitMessage = context.sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
        } catch (ignored) {
            lastCommitMessage = "Unknown"
        }
        return "*Last commit:* ```${lastCommitMessage}```"
    }

    private createJobLinkButton() {
        def button = new LinkButton()
        button.setText(new Text("Job", "plain_text"))
        button.setUrl("${context.env.BUILD_URL}")
        return button
    }

    private createJobConsoleButton() {
        def button = new LinkButton()
        button.setText(new Text("Console", "plain_text"))
        button.setUrl("${context.env.BUILD_URL}console")
        return button
    }

    private createTestResultsButton() {
        def button = new LinkButton()
        button.setText(new Text("Test results", "plain_text"))
        button.setUrl("${context.env.BUILD_URL}testReport")
        return button
    }

    private createApproveButton() {
        def button = new LinkButton()
        button.setText(new Text("Approve", "plain_text"))
        button.setUrl("${context.env.BUILD_URL}input")
        return button
    }

}



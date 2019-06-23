package com.nextiva.slack

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.nextiva.slack.dto.SlackMessage
import com.nextiva.slack.dto.attachments.Attachment
import com.nextiva.slack.dto.blocks.Actions
import com.nextiva.slack.dto.blocks.Block
import com.nextiva.slack.dto.blocks.Context
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
        List<Block> blocks = new ArrayList<>()

        Section title = new Section()
        title.setText(new Text(createBuildInfoTitle()))
        blocks.add(title)

        if(context.env.CHANGE_URL) {
            // if PR then add PR title in the message
            Section prTitle = new Section()
            prTitle.setText(new Text(createPrTitle()))
            blocks.add(prTitle)
        }

        Section buildInfo = new Section()
        buildInfo.setText(new Text(createStatus() + "\t" + createBuildBranch() + "\t" + createCommitLink()))
        blocks.add(buildInfo)

        if (!errorMessage.isEmpty()) {
            Section error = new Section()
            error.setText(new Text(createErrorMessage(errorMessage)))
            blocks.add(error)
        }

        if (hasTestResults()) {
            Section testResults = new Section()
            testResults.setText(new Text(createTestResults()))
            blocks.add(testResults)
        }

        Context commitAuthor = new Context()
        String commitAuthorName = context.common.getCommitAuthorName()
        commitAuthor.setElements(ImmutableList.of(new Text("Commit author: ${commitAuthorName}")))
        blocks.add(commitAuthor)

        Actions buttons = new Actions()
        buttons.setElements(Lists.newArrayList(createJobConsoleButton()))
        blocks.add(buttons)

        if (hasTestResults()) {
            buttons.getElements().add(createTestResultsButton())
        }

        Attachment attachment = new Attachment()
        attachment.setBlocks(blocks)
        attachment.setColor("${SLACK_NOTIFY_COLORS.get(context.currentBuild.currentResult)}")

        def message = new SlackMessage()
        message.setAttachments(ImmutableList.of(attachment))

        return message
    }

    def buildApproveMessage2(String title,
                            String text = "Stage ${context.env.STAGE_NAME} in ${getJobName()} is waiting for your approval",
                            String approveButtonText = "Approve",
                            String color = "#022ef2") {
        List<Block> blocks = new ArrayList<>()

        Section titleSection = new Section()
        titleSection.setText(new Text(title))
        blocks.add(titleSection)

        Section mainText = new Section()
        mainText.setText(new Text(text))
        blocks.add(mainText)

        Actions buttons = new Actions()
        buttons.setElements(ImmutableList.of(createApproveButton(approveButtonText)))
        blocks.add(buttons)

        Attachment attachment = new Attachment()
        attachment.setBlocks(blocks)
        attachment.setColor(color)

        def message = new SlackMessage()
        message.setAttachments(ImmutableList.of(attachment))

        return message
    }

    private createApproveText() {
        return "Stage ${context.env.STAGE_NAME} in ${getJobName()} is waiting for your approval"
    }

    def buildApproveMessage(title, String text = 'text', String color = "#022ef2") {
        List<Block> blocks = new ArrayList<>()

        Section titleSection = new Section()
        titleSection.setText(new Text(title))
        blocks.add(titleSection)

        Section mainText = new Section()
        mainText.setText(new Text(text))
        blocks.add(mainText)

        Actions buttons = new Actions()
        buttons.setElements(ImmutableList.of(createApproveButton()))
        blocks.add(buttons)

        Attachment attachment = new Attachment()
        attachment.setBlocks(blocks)
        attachment.setColor()

        def message = new SlackMessage()
        message.setAttachments(ImmutableList.of(attachment))

        return message
    }

    def buildDeployStartMessage(String appName, String version, String environment, String triggeredBy) {
        List<Block> blocks = new ArrayList<>()

        Section mainText = new Section()
        mainText.setText(new Text(createDeployStartText(appName, version, environment, triggeredBy)))
        blocks.add(mainText)

        Attachment attachment = new Attachment()
        attachment.setBlocks(blocks)
        attachment.setColor("${SLACK_NOTIFY_COLORS.get(context.currentBuild.currentResult)}")

        def message = new SlackMessage()
        message.setAttachments(ImmutableList.of(attachment))

        return message
    }

    def buildDeployFinishMessage(String appName, String version, String environment) {
        List<Block> blocks = new ArrayList<>()

        Section mainText = new Section()
        def text = "`${appName}:${version}` ${environment.toUpperCase()} "
        if (context.currentBuild.currentResult == "SUCCESS") {
            text += "deployed"
            mainText.setText(new Text(text))
            blocks.add(mainText)

            List wishList = context.libraryResource('wishes.txt').readLines()
            def randomWish = wishList[new Random().nextInt(wishList.size())]

            Context wish = new Context()
            wish.setElements(ImmutableList.of(new Text(" :tada: ${randomWish}")))
            blocks.add(wish)

        } else {
            text += "deploy failed! :disappointed:"
            mainText.setText(new Text(text))
            blocks.add(mainText)
        }

        Attachment attachment = new Attachment()
        attachment.setBlocks(blocks)
        attachment.setColor("${SLACK_NOTIFY_COLORS.get(context.currentBuild.currentResult)}")

        def message = new SlackMessage()
        message.setAttachments(ImmutableList.of(attachment))

        return message
    }

    def buildMergeFailedMessage(String sourceBranch, String destinationBranch, String pullRequestLink) {
        List<Block> blocks = new ArrayList<>()

        Section title = new Section()
        title.setText(new Text("*Failed to automatically merge ${sourceBranch} into ${destinationBranch}.*"))
        blocks.add(title)

        Section text = new Section()
        text.setText(new Text("Resolve conflicts and merge pull request"))
        blocks.add(text)

        Attachment attachment = new Attachment()
        attachment.setBlocks(blocks)
        attachment.setColor("#73797a")

        Actions buttons = new Actions()
        buttons.setElements(Lists.newArrayList(createLinkButton("Link on pull request", pullRequestLink)))
        blocks.add(buttons)

        def message = new SlackMessage()
        message.setAttachments(ImmutableList.of(attachment))

        return message
    }

    def buildHotfixStartMessage(String hotfixVersion, String author) {
        List<Block> blocks = new ArrayList<>()

        Section title = new Section()
        title.setText(new Text("*Hotfix ${context.APP_NAME} ${hotfixVersion} started successfully!*"))
        blocks.add(title)

        Context authorBlock = new Context()
        authorBlock.setElements(ImmutableList.of(new Text("Author: ${author}")))
        blocks.add(authorBlock)

        Attachment attachment = new Attachment()
        attachment.setBlocks(blocks)
        attachment.setColor("${SLACK_NOTIFY_COLORS.get(context.currentBuild.currentResult)}")

        def message = new SlackMessage()
        message.setAttachments(ImmutableList.of(attachment))

        return message
    }

    def buildHotfixFinishMessage(String hotfixVersion, String author) {
        List<Block> blocks = new ArrayList<>()

        Section title = new Section()
        title.setText(new Text("*Hotfix ${context.APP_NAME} ${hotfixVersion} finished successfully!*"))
        blocks.add(title)

        Context authorBlock = new Context()
        authorBlock.setElements(ImmutableList.of(new Text("Author: ${author}")))
        blocks.add(authorBlock)

        Attachment attachment = new Attachment()
        attachment.setBlocks(blocks)
        attachment.setColor("${SLACK_NOTIFY_COLORS.get(context.currentBuild.currentResult)}")

        def message = new SlackMessage()
        message.setAttachments(ImmutableList.of(attachment))

        return message
    }

    def buildReleaseStartMessage(String hotfixVersion, String author) {
        List<Block> blocks = new ArrayList<>()

        Section title = new Section()
        title.setText(new Text("*Release ${context.APP_NAME} ${hotfixVersion} started successfully!*"))
        blocks.add(title)

        Context authorBlock = new Context()
        authorBlock.setElements(ImmutableList.of(new Text("Author: ${author}")))
        blocks.add(authorBlock)

        Attachment attachment = new Attachment()
        attachment.setBlocks(blocks)
        attachment.setColor("${SLACK_NOTIFY_COLORS.get(context.currentBuild.currentResult)}")

        def message = new SlackMessage()
        message.setAttachments(ImmutableList.of(attachment))

        return message
    }

    def buildReleaseFinishMessage(String hotfixVersion, String author) {
        List<Block> blocks = new ArrayList<>()

        Section title = new Section()
        title.setText(new Text("*Release ${context.APP_NAME} ${hotfixVersion} finished successfully!*"))
        blocks.add(title)

        Context authorBlock = new Context()
        authorBlock.setElements(ImmutableList.of(new Text("Author: ${author}")))
        blocks.add(authorBlock)

        Attachment attachment = new Attachment()
        attachment.setBlocks(blocks)
        attachment.setColor("${SLACK_NOTIFY_COLORS.get(context.currentBuild.currentResult)}")

        def message = new SlackMessage()
        message.setAttachments(ImmutableList.of(attachment))

        return message
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private createDeployStartText(String appName, String version, String environment, String triggeredBy) {
        return "`${appName}:${version}` ${environment.toUpperCase()} deploy started by <@${triggeredBy}>"
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
        def subject = "Job: <${context.env.BUILD_URL}|${getJobName()}, build #${context.env.BUILD_NUMBER}>"
        return "${mention}*${subject}*"
    }

    private getJobName() {
        return URLDecoder.decode(context.env.JOB_NAME.toString(), 'UTF-8')
    }

    private createStatus() {
        return "*Status:* `${context.currentBuild.currentResult}`"
    }

    private createBuildBranch() {
        if(context.env.CHANGE_URL){
            // if PR then add link to the Bitbucket PR overview
            return "*PR:* <${context.env.CHANGE_URL}|${context.env.CHANGE_ID}>"
        }
        else {
            // Add link to the branch
            return "*Branch:* <${createRepositoryUrl()}/browse?at=refs/heads/${context.env.BRANCH_NAME}" +
                    "|${context.env.BRANCH_NAME}>"
        }
    }

    private createCommitLink() {
        String commit = context.sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
        return "*Last commit:* <${createRepositoryUrl()}/commits/${commit}|${commit.substring(0, 5)}>"
    }

    private createRepositoryUrl() {
        String gitUrl = context.sh(returnStdout: true, script: 'git config remote.origin.url').trim()
        String repoName = gitUrl.split('/')[-1].replaceAll('.git', '')
        String projectName = gitUrl.split('/')[-2]
        return "${BITBUCKET_URL}/projects/${projectName}/repos/${repoName}"
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
        return "*Test results:* ${summary}"
    }

    private hasTestResults() {
        return context.currentBuild.rawBuild.getAction(AbstractTestResultAction.class) != null
    }

    private createJobConsoleButton() {
        return createLinkButton("Console Output", "${context.env.BUILD_URL}console")
    }

    private createTestResultsButton() {
        return createLinkButton("Test results", "${context.env.BUILD_URL}testReport")
    }

    private createApproveButton(String text = "Approve") {
        return createLinkButton(text, "${context.env.BUILD_URL}input")
    }

    private static createLinkButton(String text, String link) {
        def button = new LinkButton()
        button.setText(new Text(text, "plain_text"))
        button.setUrl(link)
        return button
    }

    private createPrTitle() {
        String prTitle = context.bitbucket.getTitleFromPr(context.env.CHANGE_URL)
        return "*Title:* ${prTitle}"
    }
}

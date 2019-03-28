package com.nextiva.slack

import com.nextiva.slack.dto.SlackMessage
import com.nextiva.slack.dto.blocks.Block
import com.nextiva.slack.dto.blocks.Section
import com.nextiva.slack.dto.composition.Text
import com.nextiva.slack.dto.blocks.Divider

class MessagesFactory implements Serializable {

    static def buildStatusMessage(context) {
        List<Block> blocks = new ArrayList<>()

        blocks.add(new Divider())

        Section section = new Section()
        section.setText(new Text(getBuildStatusTitle(context)))
        blocks.add(section)

        def message = new SlackMessage()
        message.setBlocks(blocks)

        return message
    }

    private static getBuildStatusTitle(context) {
        def mention = ''
        def buildStatus = context.currentBuild.currentResult
        if (buildStatus ==~ "FAILURE" && context.env.BRANCH_NAME ==~ /^(release\/.+|dev|master)$/) {
            mention = "@here "
        }
        String jobName = URLDecoder.decode(context.env.JOB_NAME.toString(), 'UTF-8')
        def subject = "Job: ${jobName}, build #${context.env.BUILD_NUMBER}"
        return "${mention}*${subject}*"
    }

}



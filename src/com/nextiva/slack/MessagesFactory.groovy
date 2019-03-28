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
        Text text = new Text("Job: qa-be-integration/develop, build #1859")
        section.setText(text)
        blocks.add(section)

        def message = new SlackMessage()
        message.setBlocks(blocks)

        return message
    }

}



package com.nextiva.slack

import com.nextiva.slack.dto.SlackMessage
import com.nextiva.slack.dto.blocks.Block
import com.nextiva.slack.dto.blocks.Section
import com.nextiva.slack.dto.composition.Text
import com.nextiva.slack.dto.blocks.Divider

//import groovy.json.JsonGenerator
import groovy.json.JsonOutput

import static com.nextiva.SharedJobsStaticVars.*

class MessagesFactory implements Serializable {
//    static final def JSON_OUTPUT = new JsonGenerator.Options().excludeNulls().build()

    static def buildStatusMessage(String channel, context) {
        List<Block> blocks = new ArrayList<>()

        blocks.add(new Divider())

        Section section = new Section()
        Text text = new Text("Job: qa-be-integration/develop, build #1859")
        section.setText(text)
        blocks.add(section)

        def message = buildBaseMessage(channel)
        message.setBlocks(blocks)

        return JsonOutput.toJson(message)
    }

    private static buildBaseMessage(String channel) {
        def message = new SlackMessage()
        message.setChannel(channel)
//        message.setToken(SLACK_BOT_TOKEN)
        return message
    }
}



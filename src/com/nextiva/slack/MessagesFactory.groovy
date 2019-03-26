package com.nextiva.slack

import groovy.json.JsonGenerator
import com.nextiva.slack.dto.*

class MessagesFactory {
    static final def JSON_OUTPUT = new JsonGenerator.Options().excludeNulls().build()

    static def buildStatusMessage(context) {
        List<Block> blocks = new ArrayList<>()

        blocks.add(new Divider())

        Section section = new Section()
        Text text = new Text("Job: qa-be-integration/develop, build #1859")
        section.setText(text)
        blocks.add(section)

        return JSON_OUTPUT.toJson(new SlackMessageBuilder().blocks(blocks).color("good").build())
    }
}



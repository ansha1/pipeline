package com.nextiva.slack

import com.nextiva.slack.dto.SlackMessageBuilder
import com.nextiva.slack.dto.blocks.Block
import com.nextiva.slack.dto.blocks.Section
import com.nextiva.slack.dto.composition.Text
import com.nextiva.slack.dto.blocks.Divider

//import groovy.json.JsonGenerator
import groovy.json.JsonOutput

class MessagesFactory implements Serializable {
//    static final def JSON_OUTPUT = new JsonGenerator.Options().excludeNulls().build()

    static def buildStatusMessage(context) {
        List<Block> blocks = new ArrayList<>()

        blocks.add(new Divider())

        Section section = new Section()
        Text text = new Text("Job: qa-be-integration/develop, build #1859")
        section.setText(text)
        blocks.add(section)

        return JsonOutput.toJson(new SlackMessageBuilder().blocks(blocks).color("good").build())
    }
}



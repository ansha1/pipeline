package com.nextiva.slack.dto

import com.nextiva.slack.dto.blocks.Block
import groovy.transform.builder.Builder
import groovy.transform.builder.ExternalStrategy

class SlackMessage implements Serializable {
    List<Block> blocks = new ArrayList<>()
    String color //good (green), warning (yellow), danger (red)
}
@Builder(builderStrategy = ExternalStrategy, forClass = SlackMessage)
class SlackMessageBuilder implements Serializable {}

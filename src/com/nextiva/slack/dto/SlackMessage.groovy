package com.nextiva.slack.dto

import com.nextiva.slack.dto.blocks.Block

class SlackMessage implements Serializable {
    List<Block> blocks = new ArrayList<>()
    String color //good (green), warning (yellow), danger (red)
}
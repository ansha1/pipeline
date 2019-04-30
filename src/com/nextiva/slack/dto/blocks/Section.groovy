package com.nextiva.slack.dto.blocks

import com.nextiva.slack.dto.composition.Text

class Section extends Block {
    Section() {
        type = "section"
    }
    Text text
    List<Text> fields
}
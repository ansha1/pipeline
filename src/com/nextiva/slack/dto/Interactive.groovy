package com.nextiva.slack.dto

import com.nextiva.slack.dto.Text

abstract class Button {
    def type = "type"
}

class LinkButton extends Button {
    Text text
    String url
}

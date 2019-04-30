package com.nextiva.slack.dto.composition

class Text implements Serializable {
    String type
    String text

    Text(text, type = "mrkdwn") {
        this.text = text
        this.type = type
    }

}

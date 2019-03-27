package com.nextiva.slack.dto.composition

class Text implements Serializable {
    String type
    String text
//    boolean emoji = true
//    boolean verbatim = false

    Text(text, type = "mrkdwn") {
        this.text = text
        this.type = type
    }

}

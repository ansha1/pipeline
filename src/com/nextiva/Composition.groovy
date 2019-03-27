package com.nextiva

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

class ConfirmationDialog implements Serializable {
    Text title
    Text text
    Text confirm
    Text deny
}

class Option implements Serializable {
    Text text
    String value
}

class Options implements Serializable {
    Text label
    List<Option> options
}
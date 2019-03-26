package slack.dto

class Text {
    String type
    String text
//    boolean emoji = true
//    boolean verbatim = false

    Text(text, type = "mrkdwn") {
        this.text = text
        this.type = type
    }

}

class ConfirmationDialog {
    Text title
    Text text
    Text confirm
    Text deny
}

class Option {
    Text text
    String value
}

class Options {
    Text label
    List<Option> options
}
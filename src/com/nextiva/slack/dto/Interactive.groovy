package slack.dto

abstract class Button {
    def type = "type"
}

class LinkButton extends Button {
    Text text
    String url
}

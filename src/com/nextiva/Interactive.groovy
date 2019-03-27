package com.nextiva

abstract class Button implements Serializable {
    def type = "type"
}

class LinkButton extends Button {
    Text text
    String url
}

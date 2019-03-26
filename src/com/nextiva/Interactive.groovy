package com.nextiva

abstract class Button {
    def type = "type"
}

class LinkButton extends Button {
    Text text
    String url
}

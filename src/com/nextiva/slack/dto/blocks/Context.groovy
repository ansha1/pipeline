package com.nextiva.slack.dto.blocks

class Context extends Block {
    Context() {
        type = "context"
    }
    def elements //An array of image elements and text objects.
}
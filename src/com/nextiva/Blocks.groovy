package com.nextiva

abstract class Block implements Serializable {
    def type
    def block_id
}

class Divider extends Block {
    Divider() {
        type = "divider"
    }
}

class Section extends Block {
    Section() {
        type = "section"
    }
    Text text
    List<Text> fields
}

class Image extends Block {
    Image() {
        type = "image"
    }
    Text title
    String image_url
    String alt_text
}

class Actions extends Block {
    Actions() {
        type = "actions"
    }
    def elements //An array of interactive element objects - buttons, select menus, overflow menus, or date pickers
}

class Context extends Block {
    Context() {
        type = "context"
    }
    def elements //An array of image elements and text objects.
}
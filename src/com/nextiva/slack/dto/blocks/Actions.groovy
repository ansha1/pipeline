package com.nextiva.slack.dto.blocks

class Actions extends Block {
    Actions() {
        type = "actions"
    }
    def elements //An array of interactive element objects - buttons, select menus, overflow menus, or date pickers
}

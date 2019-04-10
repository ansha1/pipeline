package com.nextiva.slack.dto.interactive

abstract class Button implements Serializable {
    def type = "button"
    def value = "click_me_123"
    def action_id
}
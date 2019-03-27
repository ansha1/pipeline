package com.nextiva.slack.dto

import com.nextiva.Block

class Divider extends Block implements Serializable {
    Divider() {
        type = "divider"
    }
}
package com.nextiva.slack.dto.blocks

import com.nextiva.slack.dto.composition.Text

class Image extends Block {
    Image() {
        type = "image"
    }
    Text title
    String image_url
    String alt_text
}

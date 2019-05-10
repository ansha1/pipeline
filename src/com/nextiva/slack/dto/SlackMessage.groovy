package com.nextiva.slack.dto

import com.nextiva.slack.dto.attachments.Attachment
import com.nextiva.slack.dto.blocks.Block

class SlackMessage implements Serializable {
    String channel
    String text = ''
    Boolean as_user = false
    @Deprecated
    List<Attachment> attachments
    List<Block> blocks
    String icon_emoji
    String icon_url
    Boolean mrkdwn = true
    String parse = "none"
    Boolean reply_broadcast = false
    def thread_ts
    Boolean unfurl_links = false
    Boolean unfurl_media = true
    String username
}

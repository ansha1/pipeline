package com.nextiva.slack.dto.blocks

abstract class Block implements Serializable {
    def type
    def block_id
}
package com.nextiva.build.tool

import com.nextiva.utils.Logger

class Npm extends BuildTool{
    Logger log = new Logger(this)

    Npm(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
    }
}

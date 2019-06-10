package com.nextiva.build.tool

import com.nextiva.utils.Logger

class Pip extends BuildTool{
    Logger log = new Logger(this)

    Pip(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
    }
}

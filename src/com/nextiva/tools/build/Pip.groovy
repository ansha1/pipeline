package com.nextiva.tools.build

import com.nextiva.utils.Logger

class Pip extends BuildTool{
    Logger log = new Logger(this)

    Pip(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
    }

    @Override
    void setVersion(String version) {

    }

    @Override
    String getVersion() {
        return null
    }

    @Override
    void sonarScan() {

    }

    @Override
    void securityScan() {

    }

    @Override
    void publish() {

    }

    @Override
    Boolean isArtifactAvailableInRepo() {
        return null
    }
}

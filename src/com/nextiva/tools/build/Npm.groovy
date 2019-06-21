package com.nextiva.tools.build

import com.nextiva.utils.Logger

class Npm extends BuildTool{
    Logger log = new Logger(this)

    Npm(Script script, Map toolConfiguration) {
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

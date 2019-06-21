package com.nextiva.tools.build

import com.nextiva.utils.Logger

class Docker extends BuildTool{
    Logger log = new Logger(this)

    Docker(Script script, Map toolConfiguration) {
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

package com.nextiva.tools.build

import com.nextiva.utils.Logger

class Docker extends BuildTool{

    Docker(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
    }

    @Override
    String getVersion() {
        return null
    }

    @Override
    Boolean setVersion(String version) {
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

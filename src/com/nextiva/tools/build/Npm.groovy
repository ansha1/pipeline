package com.nextiva.tools.build

class Npm extends BuildTool {

    Npm(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
    }

    @Override
    void setVersion(String version) {
        // TODO: implement version
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

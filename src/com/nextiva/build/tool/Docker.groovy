package com.nextiva.build.tool

class Docker extends BuildTool{

    Docker(Script script, String name, String pathToSrc, Object buildCommands, Object postBuildCommands, Object unitTestCommands, Object postUnitTestCommands, Object integrationTestCommands, Object postIntegrationTestCommands, Boolean publishArtifact) {
        super(script, name, pathToSrc, buildCommands, postBuildCommands, unitTestCommands, postUnitTestCommands, integrationTestCommands, postIntegrationTestCommands, publishArtifact)
    }

    @Override
    void setVersion(String version) {
        //replace + sign if available since + is not allowed in a Docker tag

    }

    @Override
    String getVersion() {
        //replace + sign if available since + is not allowed in a Docker tag
        return null
    }

    @Override
    Boolean build() {
        return null
    }

    @Override
    Boolean unitTest() {
        return null
    }

    @Override
    Boolean integrationTest() {
        return null
    }

    @Override
    Boolean publish() {
        return null
    }

    @Override
    Boolean isArtifactAvailableInRepo() {
        return null
    }
}

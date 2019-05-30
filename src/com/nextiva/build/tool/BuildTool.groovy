package com.nextiva.build.tool

abstract class BuildTool {
    String name
    String pathToSrc
    def buildCommands
    def postBuildCommands
    def unitTestCommands
    def postUnitTestCommands
    def integrationTestCommands
    def postIntegrationTestCommands
    Boolean publishArtifact

    BuildTool(Script script, String name, String pathToSrc, def buildCommands, def postBuildCommands, def unitTestCommands,
              def postUnitTestCommands, def integrationTestCommands, def postIntegrationTestCommands, Boolean publishArtifact) {

    }


    abstract void setVersion(String version)

    abstract String getVersion()

    abstract Boolean build()

    abstract Boolean unitTest()

    abstract Boolean integrationTest()

    abstract Boolean publish()

    abstract Boolean isArtifactAvailableInRepo()

//    protected echo(msg){
////        script.echo("[${this.getClass().getSimpleName()}] ${msg}")
////    }
}
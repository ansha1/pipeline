package com.nextiva.build.tool

import static com.nextiva.Utils.shOrClosure

abstract class BuildTool {
    String tool
    String pathToSrc
    def buildCommands
    Boolean publishArtifact
    String image
    String resourceRequestCpu
    String resourceLimitCpu
    String resourceRequestMemory
    String resourceLimitMemory

    void setVersion(version)
    String getVersion()
    void build(){
        shOrClosure(script, buildCommands)
    }
    void publish()
    void isArtifactAvailableInRepo()

//    protected echo(msg){
////        script.echo("[${this.getClass().getSimpleName()}] ${msg}")
////    }
}
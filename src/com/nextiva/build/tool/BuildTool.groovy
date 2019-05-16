package com.nextiva.build.tool

interface BuildTool {
    void setVersion(version)
    String getVersion()
    void build()
    void publish()
    void isArtifactAvailableInRepo()

//    protected echo(msg){
////        script.echo("[${this.getClass().getSimpleName()}] ${msg}")
////    }
}
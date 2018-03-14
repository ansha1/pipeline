def call() {
    def buildFileName = 'build.properties'
    def buildPropertiesVar = "/* build properties /*/n"
    buildPropertiesVar += generateBuildInfo(deployEnvironment, version, jobName)
    println buildPropertiesVar
    writeFile file: buildFileName, text: buildPropertiesVar
}

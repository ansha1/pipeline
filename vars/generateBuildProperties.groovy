def call() {
    def buildFileName = 'build.properties'
    def buildPropertiesVar = '/* build properties /*'
    buildPropertiesVar += "/n" + generateBuildInfo()
    println buildPropertiesVar
    writeFile file: buildFileName, text: buildPropertiesVar
}

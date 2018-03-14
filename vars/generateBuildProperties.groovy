def call() {
    def buildFileName = 'build.properties'
    def buildPropertiesVar = '/* build properties /*'
    buildPropertiesVar << generateBuildInfo()
    println buildPropertiesVar
    //writeFile file: buildFileName, text: path
}

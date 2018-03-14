def call(){
    def buildFileName = "${env.WORKSPACE}" + "/" + "build.properties"
    def createBuildFileName = new File(buildFileName).createNewFile()
    def writeToFile = createBuildFileName.newWriter()
    
    sh "echo writing build info to ${BUILD_FILENAME} to ${env.WORKSPACE}"
    writeToFile << generateBuildInfo()

    writeToFile.close()
}

def call(){
    def buildFileName = "${env.WORKSPACE}" + "/" + "build.properties"
    sh "echo ${buildFileName}"
    def createBuildFileName = new File(buildFileName)
    
    sh "echo writing build info to ${BUILD_FILENAME} to ${env.WORKSPACE}"
    createBuildFileName.newWriter().withWriter { w ->
      w << generateBuildInfo()
    }
}

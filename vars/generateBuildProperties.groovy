def call(){
    def buildFileName = "${env.WORKSPACE}" + "/" + "build.properties"
    sh "echo ${buildFileName}"
    def createBuildFileName = new File(buildFileName)
    
    sh "echo writing build info to ${buildFileName} to ${env.WORKSPACE}"
    createBuildFileName.newWriter().withWriter { w ->
      w << generateBuildInfo()
    }
}

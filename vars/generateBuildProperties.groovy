def call(){
    def buildFileName = "build.properties"
    sh "echo ${buildFileName} && cd ${env.WORKSPACE} && touch ${buildFileName}"

    def createBuildFileName = new File(buildFileName)
    
    sh "echo writing build info to ${buildFileName} to ${env.WORKSPACE}"
    createBuildFileName.newWriter().withWriter { w ->
      w << /*generateBuildInfo()*/ "Hehe we have written something"
    }
}

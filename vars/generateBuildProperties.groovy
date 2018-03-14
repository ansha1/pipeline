def call(){
    def buildFileName = "build.properties"
    sh "echo ${buildFileName} && cd ${env.WORKSPACE} && touch ${buildFileName}"
    sh "echo writing build info to ${buildFileName} to ${env.WORKSPACE}"

    def file = new File(buildFileName)
    def w = file.newWriter()
    w << "Another way"
    w.close()

}

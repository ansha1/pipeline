def call() {
    def buildFileName = "build.properties"
    def path = "${env.WORKSPACE}" + "/" + buildFileName
    //sh "echo ${buildFileName} && cd ${env.WORKSPACE}"
    //sh "echo writing build info to ${buildFileName} to ${env.WORKSPACE}"
    writeFile file: buildFileName, text: path



//    buildFileName << generateBuildInfo()
/*
    def w = file.newWriter()
    w << "Another way"
    w.close()
*/


}

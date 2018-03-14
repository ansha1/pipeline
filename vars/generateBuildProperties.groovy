def call() {
    def buildFileName = "build.properties"
    sh "echo ${buildFileName} && cd ${env.WORKSPACE}"
    sh "echo writing build info to ${buildFileName} to ${env.WORKSPACE}"
    writeFile file: 'build.properties', text: '/* build.properties */'
    buildFileName << generateBuildInfo()
/*
    def w = file.newWriter()
    w << "Another way"
    w.close()
*/


}

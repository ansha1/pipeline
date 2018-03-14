def call() {
    def buildFileName = "build.properties"
    sh "echo ${buildFileName} && cd ${env.WORKSPACE}"
    sh "echo writing build info to ${buildFileName} to ${env.WORKSPACE}"
/*
    writeFile file: 'build.properties', text: '/* build.properties */'
    def w = file.newWriter()
    w << "Another way"
    w.close()
*/

if(build.workspace.isRemote()){
channel = build.workspace.channel
println "we definately are running on slave"
}
String fp = build.workspace.toString() + "/" + "newfile.txt"
newFile = new hudson.FilePath(channel, fp)
newFile.write("xyz", null)

}

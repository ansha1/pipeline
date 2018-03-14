def call(){
    def buildFileName = "build.properties"
    sh "echo ${buildFileName} && cd ${env.WORKSPACE} && touch ${buildFileName}"
    sh "echo writing build info to ${buildFileName} to ${env.WORKSPACE}"

targetFile = new File(buildFileName);
println "attempting to create file: $targetFile"

if (targetFile.createNewFile()) {
    println "Successfully created file $targetFile"
} else {
    println "Failed to create file $targetFile"
}

/*
    def file = new File(buildFileName)
    def w = file.newWriter()
    w << "Another way"
    w.close()
*/
}

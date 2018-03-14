def call(){
    def buildFileName = "build.properties"
    sh "echo ${buildFileName} && cd ${env.WORKSPACE}"
    sh "echo writing build info to ${buildFileName} to ${env.WORKSPACE}"

targetFile = new File(buildFileName);
println "attempting to create file: $targetFile"
println "the result: " + targetFile.createNewFile()

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

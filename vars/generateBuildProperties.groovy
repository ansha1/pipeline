import hudson.FilePath;
import jenkins.model.Jenkins;

def call() {
    def buildFileName = "build.properties"
    def path = "${env.WORKSPACE}" + "/" + buildFileName
    sh "echo ${buildFileName} && cd ${env.WORKSPACE}"
    //sh "echo writing build info to ${buildFileName} to ${env.WORKSPACE}"
    //writeFile file: 'build.properties', text: '/* build.properties */'

    println Jenkins.getInstance().getComputer(env['NODE_NAME'])
    def createBuildFileName = new FilePath(Jenkins.getInstance().getComputer(env['NODE_NAME']).getChannel(), path);
    createBuildFileName.newWriter().withWriter { w ->
      w << "HEHE
    }


//    buildFileName << generateBuildInfo()
/*
    def w = file.newWriter()
    w << "Another way"
    w.close()
*/


}

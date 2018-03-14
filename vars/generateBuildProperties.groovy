import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.util.*

def call() {
    def buildFileName = "build.properties"
    def path = "${env.WORKSPACE}" + "/" + buildFileName
    //sh "echo ${buildFileName} && cd ${env.WORKSPACE}"
    //sh "echo writing build info to ${buildFileName} to ${env.WORKSPACE}"
    //writeFile file: 'build.properties', text: '/* build.properties */

manager.listener.logger.println manager.build.project.getWorkspace()
manager.listener.logger.println manager.build.workspace

if (manager.build.workspace.isRemote()){
    channel = manager.build.workspace.channel
    manager.listener.logger.println  "I AM REMOTE!!"
}

fp = manager.build.workspace.toString() + "/repo_name/" + "mydeps.file"
newFile = new hudson.FilePath(channel, fp)


//    buildFileName << generateBuildInfo()
/*
    def w = file.newWriter()
    w << "Another way"
    w.close()
*/


}

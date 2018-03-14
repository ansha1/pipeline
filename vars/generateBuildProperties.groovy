import hudson.FilePath;
import jenkins.model.Jenkins;

def call() {
    def buildFileName = "build.properties"
    def path = "${env.WORKSPACE}" + "/" + buildFileName
    //sh "echo ${buildFileName} && cd ${env.WORKSPACE}"
    //sh "echo writing build info to ${buildFileName} to ${env.WORKSPACE}"
    //writeFile file: 'build.properties', text: '/* build.properties */'

if(build.workspace.isRemote())
{
    channel = build.workspace.channel;
    fp = new FilePath(channel, build.workspace.toString() + "/node_details.txt")
} else {
    fp = new FilePath(new File(build.workspace.toString() + "/node_details.txt"))
}

if(fp != null)
{
    fp.write("test data", null); //writing to file
} 

    }


//    buildFileName << generateBuildInfo()
/*
    def w = file.newWriter()
    w << "Another way"
    w.close()
*/


}

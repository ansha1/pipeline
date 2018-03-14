import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*

def call() {
    def buildFileName = "build.properties"
    def path = "${env.WORKSPACE}" + "/" + buildFileName
    //sh "echo ${buildFileName} && cd ${env.WORKSPACE}"
    //sh "echo writing build info to ${buildFileName} to ${env.WORKSPACE}"
    //writeFile file: 'build.properties', text: '/* build.properties */'

	if(manager.build.workspace.isRemote())
	{
    	channel = manager.build.workspace.channel;
	}

	// get build workspace path
	fp = new hudson.FilePath(channel, manager.build.workspace.toString())

	println fp

//    buildFileName << generateBuildInfo()
/*
    def w = file.newWriter()
    w << "Another way"
    w.close()
*/


}

import static com.nextiva.SharedJobsStaticVars.*
/** 
*
* build Deb pkg without deploying to Nexus
*
* deployEnvironment - The environment to build for. Examples: "dev", "staging". If there isn't 
*                     an existing repo for the specified environment, one will be created.
*
* packageName - What should the name of the built package be? Examples: "analytics", "realtalk".
*
* extraPath -  Any extra path we need to `cd` into before we can begin running `dpkg` commands. 
*              For example, analytics requires us to be in the "backend" dir, not the root of 
*              the project.
*
* version - The version that should be used as a default value for the packages that will be 
*           created first-time.
*
**/


def build(String packageName, String version, String deployEnvironment, String extraPath = "./") {

    def buildLocation = "${env.WORKSPACE}" + "/" + extraPath

    def gitCommit = sh returnStdout: true, script: '''echo "$(git rev-parse HEAD)"'''
    def setPackageMessage = 'autoincremented from git revision ' + gitCommit

    println "GIT commit - " + gitCommit

    generateBuildProperties(deployEnvironment, version, "${env.JOB_NAME}")
    sh """
          cd ${buildLocation} && rm -f ../${packageName}*.deb ../${packageName}*.dsc ../${packageName}*.changes ../${packageName}*.tar.gz
          dch --check-dirname-level=0 -b -v ${version}~${deployEnvironment} -M ${setPackageMessage}
          dpkg-buildpackage -us -uc -b
    """
}


def publish(String packageName, String deployEnvironment, String extraPath = "./") {

    def buildLocation = "${env.WORKSPACE}" + "/" + extraPath
    def nexusDebRepoUrl = NEXUS_DEB_PKG_REPO_URL + deployEnvironment + "/"
    println "nexusDebRepoUrl: " + nexusDebRepoUrl

    if (deployEnvironment in LIST_OF_ENVS) {
        println "Deploy deb-package to Nexus (repo: " + deployEnvironment + ")"
        sh "cd ${buildLocation}/../"
        def debName = sh returnStdout: true, script: '''echo "$(ls -1 ${packageName}*.deb | head -n 1|xargs)"'''
        debName = debName.trim()
        println "FOUND deb-package: " + debName
        //upload deb-package to Nexus 3
        sh 'curl -K /etc/nexus_curl_config -X POST -H ' + DEB_PKG_CONTENT_TYPE_PUBLISH + ' --data-binary @' + debName + ' ' + nexusDebRepoUrl
    }
    else {
        throw new IllegalArgumentException("Provided env ${deployEnvironment} is not in the list ${LIST_OF_ENVS}")
    }

}

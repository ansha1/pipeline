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


def build(String packageName, String version, String deployEnvironment, String extraPath = 'default') {

    def pathToDebianFolder = ''
    def buildLocation = ''

    if ( extraPath == 'default' ) { 
        pathToDebianFolder = WORKSPACE + "/" + "debian"
        buildLocation = WORKSPACE
    }
    else {
        pathToDebianFolder = WORKSPACE + "/" + extraPath + "/" + "debian"
        buildLocation = WORKSPACE + "/" + extraPath
    }

    def isDebDirPath = sh(returnStatus: true, script: """
                            if [ -d ${pathToDebianFolder} ]; then
                                return 0
                            else
                                return 1
                            fi
                            """)

    if ( isDebDirPath == 0 ) {
        def gitCommit = sh returnStdout: true, script: '''echo "$(git rev-parse HEAD)"'''
        def setPackageMessage = 'autoincremented from git revision ' + gitCommit
        generateBuildProperties(deployEnvironment, version, "${env.JOB_NAME}")
        sh """
            {
              cd ${buildLocation} && rm -vf ../${packageName}*.deb ../${packageName}*.dsc ../${packageName}*.changes ../${packageName}*.tar.gz ../${packageName}*.buildinfo
              dch --check-dirname-level=0 -b -v ${version}~${deployEnvironment} -M ${setPackageMessage}
              dpkg-buildpackage -us -uc -b
            }
        """
    }
    else {
        currentBuild.rawBuild.result = Result.ABORTED
        throw new hudson.AbortException("there is no 'debian' folder within ${buildLocation}.")
    }
}

def publish(String packageName, String deployEnvironment, String extraPath = 'default') {

    def buildLocation = ''

    if ( extraPath == 'default' ) {
        buildLocation = WORKSPACE
    }
    else {
        buildLocation = WORKSPACE + "/" + extraPath
    }

    def nexusDebRepoUrl = NEXUS_DEB_PKG_REPO_URL + deployEnvironment + "/"
    println "nexusDebRepoUrl: " + nexusDebRepoUrl

    if (deployEnvironment in LIST_OF_ENVS) {
        println "Deploy deb-package to Nexus (repo: " + deployEnvironment + ")"

        // get build version from build.properties file
        def getBuildVersion = sh(returnStdout: true, script: """cat ${BUILD_PROPERTIES_FILENAME} |grep build_version|awk -F'=' '{print \$2}'""").trim()
        println getBuildVersion
        def debName = sh(returnStdout: true, script: """cd ${buildLocation} && ls -1 ../${packageName}_${getBuildVersion}*.deb""").trim()
        println "FOUND deb-package: " + debName

        // upload deb-package to Nexus 3
        def isDeployedToNexus = sh(returnStatus: true, script: """cd ${buildLocation} && curl --silent --show-error --fail -K /etc/nexus_curl_config -X POST -H ${DEB_PKG_CONTENT_TYPE_PUBLISH} \\
                                --data-binary @${debName} ${nexusDebRepoUrl}""")
        println "Deployment to Nexus finished with status: " + isDeployedToNexus
        if ( isDeployedToNexus != 0 ) {
            currentBuild.rawBuild.result = Result.ABORTED
            throw new hudson.AbortException("there was a problem with pushing ${debName} to ${nexusDebRepoUrl}.")
        } 
    }
    else {
        throw new IllegalArgumentException("Provided env ${deployEnvironment} is not in the list ${LIST_OF_ENVS}")
    }
}

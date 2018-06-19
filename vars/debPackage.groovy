import static com.nextiva.SharedJobsStaticVars.*
/** 
*
* build Deb pkg with deploy to Nexus
*
* packageName - What should the name of the built package be? Examples: "analytics", "realtalk".
*
* version - The version that should be used as a default value for the packages that will be 
*           created first-time.
*
* deployEnvironment - The environment to build for. Examples: "dev", "staging". If there isn't 
*                     an existing repo for the specified environment, one will be created.
*
* extraPath -  Any extra path we need to `cd` into before we can begin running `dpkg` commands. 
*              For example, analytics requires us to be in the "backend" dir, not the root of 
*              the project.
*
* dockerImage - The docker image object created with "docker.build()"
*
**/


def build(String packageName, String version, String deployEnvironment, String extraPath = null, def dockerImage = null) {

    def pathToDebianFolder = ''
    def buildLocation = ''

    if ( extraPath ) {
        log.info("We are going to build within " + extraPath)
        pathToDebianFolder = WORKSPACE + "/" + extraPath + "/" + "debian"
        buildLocation = WORKSPACE + "/" + extraPath
    }
    else {
        pathToDebianFolder = WORKSPACE + "/" + "debian"
        buildLocation = WORKSPACE
    }

    if ( fileExists(pathToDebianFolder) ) {
        def gitCommit = sh returnStdout: true, script: '''echo "$(git rev-parse HEAD)"'''
        def setPackageMessage = 'autoincremented from git revision ' + gitCommit

        dir(buildLocation) {
            generateBuildProperties(deployEnvironment, version, "${env.JOB_NAME}")
        }

        def generateDebBuildString = """
            {
                export PIP_TRUSTED_HOST=${PIP_TRUSTED_HOST}
                export PIP_INDEX_URL=${PIP_EXTRA_INDEX_URL}${deployEnvironment}${PIP_EXTRA_INDEX_URL_SUFFIX}
                cd ${buildLocation} && rm -vf ../${packageName}*.deb ../${packageName}*.dsc ../${packageName}*.changes ../${packageName}*.tar.gz ../${packageName}*.buildinfo
                dch --check-dirname-level=0 -b -v ${version}~${deployEnvironment} -M ${setPackageMessage}
                dpkg-buildpackage -us -uc -b
            }
        """

        if ( ! dockerImage ) {
            sh "${generateDebBuildString}"
        }
        else {
            dockerImage.inside('-u root') {
                sh """
                {
                    apt-get update
                    apt-get install -y dh-virtualenv devscripts dh-systemd
                    ${generateDebBuildString}
                }
                """
            }
        }
    }
    else {
        currentBuild.rawBuild.result = Result.ABORTED
        throw new hudson.AbortException("ERROR: There is no 'debian' folder within ${buildLocation}.")
    }
}


def publish(String packageName, String deployEnvironment, String extraPath = null) {

    def buildLocation = ''

    if ( extraPath ) {
        buildLocation = WORKSPACE + "/" + extraPath
    }
    else {
        buildLocation = WORKSPACE
    }

    def nexusDebRepoUrl = NEXUS_DEB_PKG_REPO_URL + deployEnvironment + "/"
    log.info("nexusDebRepoUrl: " + nexusDebRepoUrl)

    if ( deployEnvironment in LIST_OF_ENVS ) {
        log.info("Deploy deb-package to Nexus (repo: " + deployEnvironment + ")")

        // get build version from build.properties file
        dir(buildLocation) {
            def buildProperties = readProperties file: BUILD_PROPERTIES_FILENAME
            def getBuildVersion = buildProperties.build_version
            log.info(getBuildVersion)
            def debName = sh(returnStdout: true, script: """ls -1 ../${packageName}_${getBuildVersion}*.deb""").trim()
            log.info("FOUND deb-package: " + debName)

            // upload deb-package to Nexus 3
            def verbose = log.isDebug() : "" ? "--verbose"
            def isDeployedToNexus = sh(returnStatus: true, script: """curl ${verbose} --silent --show-error --fail -K /etc/nexus_curl_config -X POST -H ${DEB_PKG_CONTENT_TYPE_PUBLISH} \\
                                    --data-binary @${debName} ${nexusDebRepoUrl}""")
            log.info("Deployment to Nexus finished with status: " + isDeployedToNexus)
            if ( isDeployedToNexus != 0 ) {
                currentBuild.rawBuild.result = Result.ABORTED
                throw new hudson.AbortException("ERROR: There was a problem with pushing ${debName} to ${nexusDebRepoUrl}.")
            }
        }
    }
    else {
        throw new IllegalArgumentException("ERROR: Provided env ${deployEnvironment} is not in the list ${LIST_OF_ENVS}")
    }
}


Boolean isDebPackageExists(String packageName, String packageVersion, String deployEnvironment) {
    // example of url: http://repository.nextiva.xyz/repository/apt-dev/pool/d/data-migration/data-migration_0.0.1704~dev_all.deb

    def index_char = packageName.substring(0,1)
    def nexusDebPackageUrl = "${NEXUS_DEB_PKG_REPO_URL}${deployEnvironment}/pool/${index_char}/${packageName}/${packageName}_${packageVersion}~${deployEnvironment}_all.deb"
    log.debug("Deb-package URL: " + nexusDebPackageUrl)
    def verbose = log.isDebug() : "" ? "--verbose"
    def status = sh(returnStatus: true, script: "curl ${verbose} --silent --show-error --fail -I ${nexusDebPackageUrl}")
    
    if ( status == 0 ) {
        log.info("Deb package ${packageName} with version ${packageVersion} exists in Nexus.")
        return true
    }
    else {
        log.info("Deb package ${packageName} with version ${packageVersion} not found in Nexus.")
        return false
    }
}

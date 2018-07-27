import static com.nextiva.SharedJobsStaticVars.*

Boolean isDebPackageExists(String packageName, String packageVersion, String deployEnvironment) {
    // example of url: http://repository.nextiva.xyz/repository/apt-dev/pool/d/data-migration/data-migration_0.0.1704~dev_all.deb

    def index_char = packageName.substring(0, 1)
    def nexusDebPackageUrl = "${NEXUS_DEB_PKG_REPO_URL}${deployEnvironment}/pool/${index_char}/${packageName}/${packageName}_${packageVersion}~${deployEnvironment}_all.deb"
    log.debug("Deb-package URL: " + nexusDebPackageUrl)

    def verbose = log.isDebug() ? "--verbose --include" : ""
    if (log.isDebug()) {
        log.info("nexusDebPackageUrl: ${nexusDebPackageUrl}")
    }

    def status = sh(returnStatus: true, script: "curl ${verbose} --show-error --fail -I ${nexusDebPackageUrl}")

    if (status == 0) {
        log.info("Deb package ${packageName} with version ${packageVersion} exists in Nexus.")
        return true
    } else {
        log.info("Deb package ${packageName} with version ${packageVersion} not found in Nexus.")
        return false
    }
}

Boolean checkNexusPackage(String repo, String format, String packageName, String packageVersion) {

    def nexusRestApi = "http://repository.nextiva.xyz/service/rest/beta/search?repository="
    def searchNexusQuery = nexusRestApi + repo + "&format=" + format + "&name=" + packageName + "&version=" + packageVersion

    def res = new groovy.json.JsonSlurper().parseText(new URL(searchNexusQuery).getText())
    if (log.isDebug()) {
        log.debug("searchNexusQuery: ${searchNexusQuery}")
        log.debug("The result of query: ${res}")
    }
    checkStatus(res, packageName, packageVersion)
}

Boolean checkStatus(Map searchQueryResult, String packageName, String packageVersion) {

    if (searchQueryResult.items.size() > 0) {
        log.info("Package ${packageName} with version ${packageVersion} exists in Nexus.")
        return true
    } else {
        log.info("Package ${packageName} with version ${packageVersion} not found in Nexus.")
        return false
    }
}

// example of url: http://repository.nextiva.xyz/service/rest/beta/search?repository=static-assets-production&format=raw&name=agent-0.1.21
Boolean isAssetsPackageExists(String packageName, String packageVersion, String repo, String format = 'raw') {
    checkNexusPackage(repo, format, packageName, packageVersion)
}

// example of url: http://repository.nextiva.xyz/service/rest/beta/search?repository=pypi-dev&format=pypi&name=crm-models&version=0.1.1
Boolean isPypiPackageExists(String packageName, String packageVersion, String repo, String format = 'pypi') {
    checkNexusPackage(repo, format, packageName, packageVersion)
}

// example of url: http://repository.nextiva.xyz/service/rest/beta/search?repository=docker&format=docker&name=analytics&version=0.1.504
Boolean isDockerPackageExists(String packageName, String packageVersion, String repo = 'docker', String format = 'docker') {
    checkNexusPackage(repo, format, packageName, packageVersion)
}

def uploadStaticAssets(String deployEnvironment, String assetDir, String version, String packageName) {

    def jobName = "${env.JOB_NAME}"
    def nexusRepoUrl = NEXUS_STATIC_ASSETS_REPO_URL + deployEnvironment
    def assetPath = "${env.WORKSPACE}/${packageName}-${env.EXECUTOR_NUMBER}.${ASSETS_PACKAGE_EXTENSION}"

    if (deployEnvironment in LIST_OF_ENVS) {
        generateBuildProperties(deployEnvironment, version, jobName)

        def verbose = log.isDebug() ? "--verbose --include" : ""

        sh "cd ${assetDir} && cp ${env.WORKSPACE}/${BUILD_PROPERTIES_FILENAME} ./ && tar -czvf ${assetPath} ./"
        dir(assetDir){
            uploadFile(assetPath, "${nexusRepoUrl}/${packageName}")
            uploadFile(assetPath, "${nexusRepoUrl}/${packageName}-${version}")
        }
    }
    else {
        throw new IllegalArgumentException("Provided env ${deployEnvironment} is not in the list ${LIST_OF_ENVS}")
    }
}

def uploadFile(String filePath, String repoUrl, Boolean returnStatus = false) {
    def verbose = log.isDebug() ? "--verbose --include" : ""

    withCredentials([
        file(credentialsId: 'nexus_curl_config', variable: 'NEXUS_CURL_CONFIG')
    ]) {
        sh script: """curl ${verbose} --show-error --fail --write-out "\nStatus: %{http_code}\n" -K ${NEXUS_CURL_CONFIG} --upload-file ${filePath} ${repoUrl}""", name: 'curl', returnStatus: returnStatus  
    }
}

def postFile(String filePath, String repoUrl, Boolean returnStatus = false) {
    def verbose = log.isDebug() ? "--verbose --include" : ""

    withCredentials([
        file(credentialsId: 'nexus_curl_config', variable: 'NEXUS_CURL_CONFIG')
    ]) {
        sh(name: 'curl', returnStatus: returnStatus, script: """curl ${verbose} --show-error --fail --write-out "\nStatus: %{http_code}\n" \\
                                                                -K ${NEXUS_CURL_CONFIG} -X POST -H ${DEB_PKG_CONTENT_TYPE_PUBLISH} \\
                                                                --data-binary @${filePath} ${repoUrl}""")
    }
}

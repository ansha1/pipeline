Boolean isDebPackageExists(String packageName, String packageVersion, String deployEnvironment) {
    // example of url: http://repository.nextiva.xyz/repository/apt-dev/pool/d/data-migration/data-migration_0.0.1704~dev_all.deb

    def index_char = packageName.substring(0,1)
    def nexusDebPackageUrl = "${NEXUS_DEB_PKG_REPO_URL}${deployEnvironment}/pool/${index_char}/${packageName}/${packageName}_${packageVersion}~${deployEnvironment}_all.deb"
    log.debug("Deb-package URL: " + nexusDebPackageUrl)
    
    def verbose = ''
    if( log.isDebug() ) {
        verbose = "--verbose"
    }
    
    def status = sh(returnStatus: true, script: "curl ${verbose} --silent --show-error --fail -I ${nexusDebPackageUrl}")
    
    if ( status == 0 ) {
        log.info("Deb package ${packageName} with version ${packageVersion} exists in Nexus.")
        return true
    } else {
        log.info("Deb package ${packageName} with version ${packageVersion} not found in Nexus.")
        return false
    }
}

def checkNexusPackage(String repo, String format, String packageName, String packageVersion) {

    def nexusRestApi = "http://repository.nextiva.xyz/service/rest/beta/search?repository="
    def searchNexusQuery = nexusRestApi + repo + "&format=" + format + "&name=" + packageName + "&version=" + packageVersion

    return new groovy.json.JsonSlurper().parseText(new URL(searchNexusQuery).getText())
}

Boolean checkStatus(Map searchQueryResult, String packageName, String packageVersion) {
    
    if( searchQueryResult.items.size() > 0 ) {
        log.info("Package ${packageName} with version ${packageVersion} exists in Nexus.")
        return true
    } else {
        log.info("Package ${packageName} with version ${packageVersion} not found in Nexus.")
        return false
    }
}

// example of url: http://repository.nextiva.xyz/service/rest/beta/search?repository=static-assets-production&format=raw&name=agent-0.1.21
Boolean isAssetsPackageExists(String packageName, String packageVersion, String repo, String format = 'raw') {

    def res = checkNexusPackage(repo, format, packageName, packageVersion)
    checkStatus(res, packageName, packageVersion)
}

// example of url: http://repository.nextiva.xyz/service/rest/beta/search?repository=pypi-dev&format=pypi&name=crm-models&version=0.1.1
Boolean isPypiPackageExists(String packageName, String packageVersion, String repo, String format = 'pypi') {

    def res = checkNexusPackage(repo, format, packageName, packageVersion)
    checkStatus(res, packageName, packageVersion)
}

// example of url: http://repository.nextiva.xyz/service/rest/beta/search?repository=docker&format=docker&name=analytics&version=0.1.504
Boolean isDockerPackageExists(String packageName, String packageVersion, String repo = 'docker', String format = 'docker') {

    def res = checkNexusPackage(repo, format, packageName, packageVersion)
    checkStatus(res, packageName, packageVersion)
}





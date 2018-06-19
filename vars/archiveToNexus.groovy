import static com.nextiva.SharedJobsStaticVars.*


def call(String deployEnvironment, String assetDir, String version, String packageName) {

    def jobName = "${env.JOB_NAME}"
    def nexusRepoUrl = NEXUS_STATIC_ASSETS_REPO_URL + deployEnvironment
    def assetPath = "${env.WORKSPACE}/${packageName}-${env.EXECUTOR_NUMBER}.${ASSETS_PACKAGE_EXTENSION}"

    if (deployEnvironment in LIST_OF_ENVS) {
      generateBuildProperties(deployEnvironment, version, jobName)
      def verboseParam = log.isDebug() : "" ? "--verbose"
      sh """
          cd ${assetDir} && cp ${env.WORKSPACE}/${BUILD_PROPERTIES_FILENAME} ./ && tar -czvf ${assetPath} ./
          curl ${verboseParam} --show-error --fail --write-out "\nStatus: %{http_code}\n" -K /etc/nexus_curl_config --upload-file ${assetPath} ${nexusRepoUrl}/${packageName}
          curl ${verboseParam} --show-error --fail --write-out "\nStatus: %{http_code}\n" -K /etc/nexus_curl_config --upload-file ${assetPath} ${nexusRepoUrl}/${packageName}-${version}
      """
    }
    else {
        throw new IllegalArgumentException("Provided env ${deployEnvironment} is not in the list ${LIST_OF_ENVS}")
    }
}

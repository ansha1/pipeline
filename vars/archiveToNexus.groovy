import com.nextiva.SharedJobsStaticVars

def call(def deployEnvironment, def assetDir, def version, def packageName) {

   SharedJobsStaticVars globalVars = new SharedJobsStaticVars()

   def jobName = "${env.JOB_NAME}"
   def nexusRepoUrl = globalVars.NEXUS_STATIC_ASSETS_REPO_URL + deployEnvironment
   def assetPath = "${env.WORKSPACE}" + "/" + packageName + "-" + "${env.EXECUTOR_NUMBER}" + globalVars.ASSETS_PACKAGE_EXTENSION

   if (deployEnvironment in globalVars.LIST_OF_ENVS) {
     
     	generateBuildProperties(deployEnvironment, version, jobName)
        sh """
           cd ${assetDir} && cp ${env.WORKSPACE}/${globalVars.BUILD_FILENAME} ./ && tar -czvf ${assetPath} ./
           curl --show-error --fail --write-out "\nStatus: %{http_code}\n" -v -K /etc/nexus_curl_config --upload-file ${assetPath} ${nexusRepoUrl}/${packageName}
           curl --show-error --fail --write-out "\nStatus: %{http_code}\n" -v -K /etc/nexus_curl_config --upload-file ${assetPath} ${nexusRepoUrl}/${packageName}-${version}
        """
   }
   else {
       throw new IllegalArgumentException("Provided env ${deployEnvironment} is not in the list ${globalVars.listOfEnvs}")
   }
}

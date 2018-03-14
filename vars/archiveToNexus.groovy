def call(def deployEnvironment, def assetDir, def version, def packageName) {
  
   def jobName = "${env.JOB_NAME}"
   def listOfEnvs = ['dev', 'staging', 'rc', 'production', 'test', 'qa']
   def nexusRepoUrl = "http://repository.nextiva.xyz/repository/static-assets-" + deployEnvironment
   def assetPath = "${env.WORKSPACE}" + "/" + packageName + "-" + "${env.EXECUTOR_NUMBER}" + ".bzip"

   if (deployEnvironment in listOfEnvs) {
        if (assetDir != null) {
        generateBuildProperties(deployEnvironment, version, jobName)

/*
        sh """
           cd ${assetDir} && tar -czvf ${assetPath} ./
           curl --show-error --fail --write-out "\nStatus: %{http_code}\n" -v -K /etc/nexus_curl_config --upload-file ${assetPath} ${nexusRepoUrl}/${packageName}
           curl --show-error --fail --write-out "\nStatus: %{http_code}\n" -v -K /etc/nexus_curl_config --upload-file ${assetPath} ${nexusRepoUrl}/${packageName}-${version}
        """
*/
        }
   }
   else {
       throw new IllegalArgumentException("Provided env ${deployEnvironment} is not in the list ${listOfEnvs}")
   }
}

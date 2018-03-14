def call(def deployEnvironment, def assetDir, def version, def packageName) {
  
   def jobName = "${env.JOB_NAME}"
   def listOfEnvs = ['dev', 'staging', 'rc', 'production', 'test', 'qa']
   def nexusRepoUrl = "http://repository.nextiva.xyz/repository/static-assets-" + deployEnvironment
   def assetPath = "${env.WORKSPACE}" + "/" + packageName + "-" + "${env.EXECUTOR_NUMBER}" + ".bzip"

   if (deployEnvironment in listOfEnvs) {
        if (assetDir != null) {
//        generateBuildProperties()


	sh """
	    echo 'build_version=${version}' > build.properties
	    echo 'commit=$(git rev-parse HEAD)' >> build.properties
	    echo 'deploy_environment=${deployEnvironment}' >> build.properties
	    echo 'job_name=${jobName}' >> build.properties
	    echo 'build_date_time=$(date)' >> build.properties
	    echo 'repository_url=$(git config --get remote.origin.url)' >> build.properties
    	"""
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

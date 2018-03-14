def call(def deployEnvironment, def assetDir, def version, def packageName) {
  
   def jobName = "${env.JOB_NAME}"
   def listOfEnvs = ['dev', 'staging', 'rc', 'production', 'test', 'qa']
   def nexusRepoUrl = "http://repository.nextiva.xyz/repository/static-assets-" + deployEnvironment
   def assetPath = "${env.WORKSPACE}" + "/" + packageName + "-" + "${env.EXECUTOR_NUMBER}" + ".bzip"

   if (deployEnvironment in listOfEnvs) {
        if (assetDir != null) {
//        generateBuildProperties()
	sh '''
	cat << EOF > build.properties
    	echo "build_version=${VERSION}"
    	echo "commit=$(git rev-parse HEAD)"
   	echo "deploy_environment=${DEPLOY_ENVIRONMENT}"
    	echo "job_name=${JOB_NAME}"
    	echo "build_date_time=$(date)"
    	echo "repository_url=$(git config --get remote.origin.url)"
	EOF
	'''

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

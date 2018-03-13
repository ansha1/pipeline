def call(def deployEnvironment, def assetDir, def version, def upstreamJobName, def packageName) {
  

   def assetPath = "${env.EXECUTOR_NUMBER}" + '.bzip'
   def jobName = upstreamJobName ?: "${env.JOB_NAME}"
   def listOfEnvs = ['dev', 'staging', 'rc', 'production', 'test', 'qa']
   def nexusRepoUrl = "http://repository.nextiva.xyz/repository/static-assets-" + deployEnvironment

   if (deployEnvironment in listOfEnvs) {
        if (assetDir != null) {
        sh '''
           cd ${assetDir}
           tar -czvf ${assetPath} ./
           curl --show-error --fail --write-out "\nStatus: %{http_code}\n" -v -K /etc/nexus_curl_config --upload-file ${assetPath} ${nexusRepoUrl}/${packageName}
           curl --show-error --fail --write-out "\nStatus: %{http_code}\n" -v -K /etc/nexus_curl_config --upload-file ${assetPath} ${nexusRepoUrl}/${packageName}-${version}
        '''
        }

   }
   else {
        return 1
   }
}

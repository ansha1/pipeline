def call(def deployEnvironment, /*def assetDir, def version,*/ def upstreamJobName/*, def packageName*/) {

   def assetPath = "${env.EXECUTOR_NUMBER}" + '.bzip'
   def jobName = upstreamJobName ?: "${env.JOB_NAME}"
   def listOfEnvs = ['dev', 'staging', 'rc', 'production', 'test', 'qa']
   def NEXUS_REPO_URL = "http://repository.nextiva.xyz/repository/static-assets-" + deployEnvironment

   if (!listOfEnvs.containts(deployEnvironment)) {
	sh "echo ${env.WORKSPACE}"
 	sh "echo ${NEXUS_REPO_URL}"
	sh "echo ${assetPath}"
	sh "echo ${jobName}"
   }
   else {
   	exit 1
   }
}

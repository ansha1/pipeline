def call(def deployEnvironment, def version, def jobName) {

    println deployEnvironment + " - " + version + " - " + jobName    

    def toReturn = sh returnStdout: true, script: '''
    echo ""
    echo "commit=$(git rev-parse HEAD)"
    echo "build_date_time=$(date)"
    echo "repository_url=$(git config --get remote.origin.url)"
    '''

    def toReturn += sh returnStdout: true, script: """
	echo ''
	echo 'build_version=${version}'
	echo 'deploy_environment=${deployEnvironment}'
	echo 'job_name=${jobName}'
    """

    return toReturn 
}

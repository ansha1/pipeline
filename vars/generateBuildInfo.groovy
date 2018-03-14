def call(def deployEnvironment, def version, def jobName) {

    def toReturn = sh returnStdout: true, script: '''
    echo ""
    echo "build_version=${version}"
    echo "commit=$(git rev-parse HEAD)"
    echo "deploy_environment=${deployEnvironment}"
    echo "job_name=${jobName}"
    echo "build_date_time=$(date)"
    echo "repository_url=$(git config --get remote.origin.url)"
    '''

    return toReturn 
}

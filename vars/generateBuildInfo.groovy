def call(def deployEnvironment, def version, def jobName) {
/*
    sh '''
    echo "build_version=${VERSION}"
    echo "commit=$(git rev-parse HEAD)"
    echo "deploy_environment=${DEPLOY_ENVIRONMENT}"
    echo "job_name=${JOB_NAME}"
    echo "build_date_time=$(date)"
    echo "repository_url=$(git config --get remote.origin.url)"
    '''
*/

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

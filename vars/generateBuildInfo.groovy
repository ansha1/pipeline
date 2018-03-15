def call(def deployEnvironment, def version, def jobName) {

    def toReturn = """
    deploy_environment=${deployEnvironment}
    build_version=${version}
    job_name=${jobName}
    """
    toReturn = toReturn.replaceAll(" ", "")

    toReturn += sh returnStdout: true, script: '''
    echo "commit=$(git rev-parse HEAD)"
    echo "build_date_time=$(date)"
    echo "repository_url=$(git config --get remote.origin.url)"
    '''

    return toReturn 
}

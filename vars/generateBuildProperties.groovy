import com.nextiva.SharedJobsStaticVars

def call(def deployEnvironment, def version, def jobName) {

    SharedJobsStaticVars globalVars = new SharedJobsStaticVars()

    def buildPropertiesVar = "/* build properties /*"

    buildPropertiesVar += """
    deploy_environment=${deployEnvironment}
    build_version=${version}
    job_name=${jobName}
    """
    
    buildPropertiesVar = buildPropertiesVar.replaceAll(" ", "")

    buildPropertiesVar += sh returnStdout: true, script: '''
    echo "commit=$(git rev-parse HEAD)"
    echo "build_date_time=$(date)"
    echo "repository_url=$(git config --get remote.origin.url)"
    '''

    println buildPropertiesVar
    writeFile file: globalVars.BUILD_FILENAME, text: buildPropertiesVar
}

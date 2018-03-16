import static com.nextiva.SharedJobsStaticVars.*


def call(String deployEnvironment, String version, String jobName) {

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
    writeFile file: BUILD_PROPERTIES_FILENAME, text: buildPropertiesVar
}

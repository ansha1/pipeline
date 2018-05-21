import static com.nextiva.SharedJobsStaticVars.*


def call(String deployEnvironment, String version, String jobName) {

    def buildProperties = readProperties file: BUILD_PROPERTIES_FILENAME

    buildProperties.deploy_environment = deployEnvironment
    buildProperties.build_version = version
    buildProperties.job_name = jobName
    buildProperties.commit = sh returnStdout: true, script: 'git rev-parse HEAD'
    buildProperties.build_date_time = sh returnStdout: true, script: 'date'
    buildProperties.repository_url = sh returnStdout: true, script: 'git config --get remote.origin.url'

    String propsToWrite = ''
    buildProperties.each {
        propsToWrite = propsToWrite + it.toString().trim() + '\n'
    }
    writeFile file: BUILD_PROPERTIES_FILENAME, text: propsToWrite
}

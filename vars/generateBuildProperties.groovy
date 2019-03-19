import static com.nextiva.SharedJobsStaticVars.*
import java.net.URLDecoder


def call(String deployEnvironment, String version, String jobName) {

    def buildProperties = readProperties file: BUILD_PROPERTIES_FILENAME

    buildProperties.deploy_environment = deployEnvironment
    buildProperties.build_version = version
    buildProperties.job_name = URLDecoder.decode(jobName, 'UTF-8')
    buildProperties.commit = common.getCurrentCommit()
    buildProperties.build_date_time = sh returnStdout: true, script: 'date'
    buildProperties.repository_url = common.getRepositoryUrl()

    String propsToWrite = ''
    buildProperties.each {
        propsToWrite = propsToWrite + it.toString().trim() + '\n'
    }
    writeFile file: BUILD_PROPERTIES_FILENAME, text: propsToWrite
}

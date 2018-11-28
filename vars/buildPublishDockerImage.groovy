import static com.nextiva.SharedJobsStaticVars.*


def call(String appName, String buildVersion, String deployEnvironment = 'docker', String extraPath = null , String dockerFileName = 'Dockerfile') {
    def buildLocation = ''

    if ( extraPath ) {
        log.info("We are going to build docker image within " + extraPath)
        buildLocation = WORKSPACE + "/" + extraPath
    }
    else {
        buildLocation = WORKSPACE
    }

    dir( buildLocation ) {
        generateBuildProperties(deployEnvironment, buildVersion, env.JOB_NAME)
    }

    docker.withRegistry(DOCKER_REGISTRY_URL, DOCKER_REGISTRY_CREDENTIALS_ID) {
        def customImage = docker.build("${appName}:${buildVersion}", "-f ${buildLocation}/${dockerFileName} --build-arg build_version=${buildVersion} ${buildLocation}")
        customImage.push()
        customImage.push("latest")

        sh "docker rmi ${customImage.id} ${DOCKER_REGISTRY}/${customImage.id} ${DOCKER_REGISTRY}/${appName}:latest"
    }
}

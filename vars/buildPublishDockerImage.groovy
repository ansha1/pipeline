import static com.nextiva.SharedJobsStaticVars.*


def call(String appName, String buildVersion, String deployEnvironment = 'docker', String extraPath = null) {
    def buildLocation = ''
    def customImage

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

    docker.withRegistry(NEXTIVA_DOCKER_REGISTRY_URL, NEXTIVA_DOCKER_REGISTRY_CREDENTIALS_ID) {
        customImage = docker.build("${appName}:${buildVersion}", "-f ${buildLocation}/Dockerfile --build-arg build_version=${buildVersion} ${buildLocation}")
        customImage.push()
        customImage.push("latest")
    }

    docker.withRegistry(TENABLE_DOCKER_REGISTRY_URL, TENABLE_DOCKER_REGISTRY_CREDENTIALS_ID) {
        customImage.tag("${TENABLE_DOCKER_REGISTRY}/${appName}:${buildVersion}")
        customImage.push("${TENABLE_DOCKER_REGISTRY}/${appName}:${buildVersion}")
    }

    log.debug("Removing Old Images")
    sh "docker rmi ${customImage.id} ${NEXTIVA_DOCKER_REGISTRY}/${customImage.id} ${TENABLE_DOCKER_REGISTRY}/${customImage.id} ${NEXTIVA_DOCKER_REGISTRY}/${appName}:latest"
}

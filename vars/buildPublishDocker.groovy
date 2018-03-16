import static com.nextiva.SharedJobsStaticVars.*


def call(String appName, String buildVersion, String extraPath='.') {
    docker.withRegistry(DOCKER_REGISTRY_URL, 'nextivaRegistry') {
        def customImage = docker.build("${appName}:${buildVersion}", "-f ${extraPath}/Dockerfile --build-arg build_version=${buildVersion} ${extraPath}")
        customImage.push()
        customImage.push("latest")

        sh "docker rmi ${customImage.id} ${DOCKER_REGISTRY}/${customImage.id} ${DOCKER_REGISTRY}/${appName}:latest"
    }
}

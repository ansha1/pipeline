def call(String APP_NAME, String BUILD_VERSION, String EXTRA_PATH = '.') {
    docker.withRegistry('https://repository.nextiva.xyz', 'nextivaRegistry') {
        def customImage = docker.build("${APP_NAME}:${BUILD_VERSION}", "-f ${EXTRA_PATH}/Dockerfile --build-arg build_version=${BUILD_VERSION} ${EXTRA_PATH}")
        customImage.push()
        customImage.push("latest")
    }
}
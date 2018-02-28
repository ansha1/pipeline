def call(String APP_NAME, String BUILD_VERSION, String EXTRA_PATH) {
    docker.withRegistry('https://repository.nextiva.xyz', 'nextivaRegistry') {
        def customImage = docker.build("${APP_NAME}:${BUILD_VERSION}", "-f ${EXTRA_PATH}/Dockerfile --build-arg build_version=${BUILD_VERSION} .${EXTRA_PATH}")
        customImage.push()
        customImage.push("latest")

//
//
//        BASE_IMAGE = "repository.nextiva.xyz/${APP_NAME}"
//        IMAGE = "${BASE_IMAGE}-${BUILD_VERSION}"
//
//        sh """
//BUILD_LOCATION="${WORKSPACE}/${EXTRA_PATH}"
//cd ${BUILD_LOCATION}
//
//docker build --build-arg build_version=${BUILD_VERSION} -t ${BASE_IMAGE} .
//        docker tag ${BASE_IMAGE} ${IMAGE}
//docker push ${BASE_IMAGE}
//docker push ${IMAGE}
//
//docker rmi ${BASE_IMAGE} ${IMAGE}
//"""
    }
}
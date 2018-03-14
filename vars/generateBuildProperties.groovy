def call(){
    BUILD_FILENAME="build.properties"

    sh "echo writing build info to ${BUILD_FILENAME} to ${env.WORKSPACE}"
    new File("${env.WORKSPACE}", BUILD_FILENAME) << generateBuildInfo()
}

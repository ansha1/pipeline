def call(){
    BUILD_FILENAME="build.properties"
    sh "echo writing build info to ${BUILD_FILENAME}"
    BUILD_FILENAME << generateBuildInfo()
}

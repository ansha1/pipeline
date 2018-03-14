def call(){
    BUILD_FILENAME="build.properties"

    BUILD_FILENAME << generateBuildInfo()
}

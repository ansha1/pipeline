import static com.nextiva.SharedJobsStaticVars.*

def call(String deployEnvironment, String assetDir, String version, String packageName) {
    
}


def uploadStaticAssetstoS3(String deployEnvironment, String buildVersion, String assetDir, String version, String packageName, String pathToSrc = '.') {
    def jobName = "${env.JOB_NAME}"
    def S3DevBucketName = "static-assets-dev"
    def S3ProdBucketName = "static-assets-production"
    def assetPath = "${env.WORKSPACE}/${packageName}-${env.EXECUTOR_NUMBER}.${ASSETS_PACKAGE_EXTENSION}"
    def pathToBuildPropertiesFile = "${env.WORKSPACE}/${pathToSrc}/${BUILD_PROPERTIES_FILENAME}" 
}
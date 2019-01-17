import static com.nextiva.SharedJobsStaticVars.*

def call(String environment, String buildVersion, String appName, String assetDir, String pathToSrc) { 
}

def uploadStaticAssetstoS3(String appName, String buildVersion, String environment, Map args, String pathToSrc = '.') {
    withAWS(credentials:'nextiva.io', region:'us-west-2') {
        def jobName = "${env.JOB_NAME}"
        def assetDir = args.get('distPath', 'dist/static')
        def pathToBuildPropertiesFile = "${pathToSrc}/${BUILD_PROPERTIES_FILENAME}"
        def S3DevBucketName = "static-assests-test"
        def S3ProdBucketName = "static-assets-production.nextiva.io"
        def S3BucketName = ""
        def buildCommands = args.get('buildCommands', "export OUTPUT_PATH=${assetDir} && npm install && npm run dist")

        if (${env.BRANCH_NAME} == "master") {
            S3BucketName = S3DevBucketName
        } else {
            S3BucketName = S3ProdBucketName
        }
        
        if (deployEnvironment in LIST_OF_ENVS) {
            generateBuildProperties(environment, buildVersion, jobName)
        } else {
            throw new IllegalArgumentException("Provided env ${environment} is not in the list ${LIST_OF_ENVS}")
        }

        s3Upload(file:"${assetDir}", bucket:"${S3BucketName}", path:"${appName}/${buildVersion}/")
        s3Upload(file:"${pathToSrc}/${BUILD_PROPERTIES_FILENAME}", bucket:"${S3BucketName}", path:"${appName}/${buildVersion}/build.properties")
    }
}

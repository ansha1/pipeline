import static com.nextiva.SharedJobsStaticVars.*

def uploadFrontToS3(String appName, String buildVersion, String environment, Map args, String pathToSrc) {
    withAWS(credentials: AWS_CREDENTIALS, region: AWS_REGION) {
        def assetDir = args.get('distPath', 'dist/static')
        def S3BucketName = ""
        def PUBLISH_STATIC_ASSETS_TO_S3 = args.get('PUBLISH_STATIC_ASSETS_TO_S3')
        log.info("PUBLISH_STATIC_ASSETS_TO_S3: ${PUBLISH_STATIC_ASSETS_TO_S3}")
        if (PUBLISH_STATIC_ASSETS_TO_S3 == "true") {      
            if (env.BRANCH_NAME == "master") {
                    S3BucketName = "${S3_PRODUCTION_BUCKET_NAME}"
            } else {
                    S3BucketName = "${S3_DEV_BUCKET_NAME}"
            }

            dir(pathToSrc) {
                    if (environment in LIST_OF_ENVS) {
                            s3Upload(file: assetDir, bucket: S3BucketName, path: "${appName}/${buildVersion}/")
                            s3Upload(file: "${pathToSrc}/${BUILD_PROPERTIES_FILENAME}", bucket: S3BucketName, path:"${appName}/${buildVersion}/${BUILD_PROPERTIES_FILENAME}")
                    } else {
                            throw new IllegalArgumentException("Provided env ${environment} is not in the list ${LIST_OF_ENVS}")
                    }

            }   
        }     
    }
}

import static com.nextiva.SharedJobsStaticVars.*

def uploadFrontToS3(String appName, String buildVersion, String environment, Map args, String pathToSrc) {
    if (environment.notIn(LIST_OF_ENVS)) {
        throw new IllegalArgumentException("Provided env ${environment} is not in the list ${LIST_OF_ENVS}")
    }

    def assetDir = args.get('distPath', 'dist/static')
    String S3BucketName = env.BRANCH_NAME.equals("master") ? S3_PRODUCTION_BUCKET_NAME : S3_DEV_BUCKET_NAME

    withAWS(credentials: AWS_CREDENTIALS, region: AWS_REGION) {
        dir(pathToSrc) {
            log.info("publishStaticAssetsToS3: ${publishToS3}")
                s3Upload(file: assetDir, bucket: S3BucketName, path: "${appName}/${buildVersion}/")
                s3Upload(file: "${pathToSrc}/${BUILD_PROPERTIES_FILENAME}", bucket: S3BucketName, path: "${appName}/${buildVersion}/${BUILD_PROPERTIES_FILENAME}")
        }
    }
}

def uploadTestResults(String appName, String jobName, String buildNumber, String objectToUpload) {
    withAWS(credentials: AWS_S3_UPLOAD_CREDENTIALS, region: AWS_REGION) {
        s3Upload(bucket: S3_TEST_REPORTS_BUCKET,
                file: objectToUpload,
                path: "${appName}/${jobName}/${buildNumber}/")
    }
}
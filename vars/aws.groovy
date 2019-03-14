import static com.nextiva.SharedJobsStaticVars.*


def uploadFrontToS3(String appName, String buildVersion, String environment, Map args, String pathToSrc) {
    if (environment.notIn(LIST_OF_ENVS)) {
        throw new IllegalArgumentException("Provided env ${environment} is not in the list ${LIST_OF_ENVS}")
    }

    def assetDir = args.get('distPath', 'dist/static')
    String S3BucketName = env.BRANCH_NAME.matches(/^(release|hotfix)\/.+$/) ? S3_PUBLIC_BUCKET_NAME : S3_PRIVATE_BUCKET_NAME

    withAWS(credentials: AWS_CREDENTIALS, region: AWS_REGION) {
        dir(pathToSrc) {
            log.info("publishStaticAssetsToS3: ${publishToS3}")
                s3Upload(file: assetDir, bucket: S3BucketName, path: "${appName}/")            
                s3Upload(file: assetDir, bucket: S3BucketName, path: "${appName}/${buildVersion}/")
                s3Upload(file: "${pathToSrc}/${BUILD_PROPERTIES_FILENAME}", bucket: S3BucketName, path: "${appName}/${BUILD_PROPERTIES_FILENAME}")
                s3Upload(file: "${pathToSrc}/${BUILD_PROPERTIES_FILENAME}", bucket: S3BucketName, path: "${appName}/${buildVersion}/${BUILD_PROPERTIES_FILENAME}")
        }
    }
}

def uploadTestResults(String appName, String jobName, String buildNumber, List objectsToUpload,
                      String reportName = 'Test Report', String reportLinkSuffix = '') {
    withAWS(credentials: AWS_S3_UPLOAD_CREDENTIALS, region: AWS_REGION) {
        objectsToUpload.each {
            try {
                s3Upload(bucket: S3_TEST_REPORTS_BUCKET,
                         file: it,
                         path: "${appName}/${jobName}/${buildNumber}/")
            } catch (e) {
                log.warning("Error with uploading test results to S3 bucket! ${e}")
            }
        }
    }

    common.tempDir("tmp_${common.getRundomInt()}") {
        """ publishHTML requires at least one exists file for publish """
        sh "echo tmp > tmp.txt"

        publishHTML([allowMissing         : true,
                     alwaysLinkToLastBuild: false,
                     keepAll              : true,
                     reportDir            : '',
                     reportFiles          : "${TEST_REPORTS_URL}/${appName}/${jobName}/${buildNumber}/${reportLinkSuffix}",
                     reportName           : reportName,
                     reportTitles         : ''])
    }
}

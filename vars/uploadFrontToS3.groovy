import static com.nextiva.SharedJobsStaticVars.*

def call(String deployEnvironment, String assetDir, String version, String packageName) { 
}

def uploadStaticAssetstoS3(String environment, String buildVersion, String appName, String assetDir, String pathToSrc) {
    withAWS(credentials:'nextiva.io', region:'us-west-2') {
        def jobName = "${env.JOB_NAME}"
        def S3DevBucketName = "static-assets-test"
        def S3ProdBucketName = "static-assets-production"
        s3Upload(file:"${assetDir}", bucket:"${S3DevBucketName}", path:"${appName}/${buildVersion}/")
    }
}

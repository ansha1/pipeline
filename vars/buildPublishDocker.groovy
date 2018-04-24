def call(String appName, String buildVersion, String extraPath='.') {
    echo('\nDEPRECATED: Use buildPublishDockerImage() method.\n')
    buildPublishDockerImage(appName, buildVersion, extraPath)
}

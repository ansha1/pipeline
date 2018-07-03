def call(String appName, String buildVersion, String extraPath='.') {
    log.warning('DEPRECATED: Use buildPublishDockerImage() method.')
    buildPublishDockerImage(appName, buildVersion, extraPath)
}

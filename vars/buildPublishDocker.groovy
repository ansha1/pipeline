def call(String appName, String buildVersion, String extraPath='.') {
    log.deprecated('Use buildPublishDockerImage() method.')
    buildPublishDockerImage(appName, buildVersion, 'docker', extraPath)
}

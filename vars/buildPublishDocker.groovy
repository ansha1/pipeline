def call(String appName, String buildVersion, String extraPath='.') {
    echo('DEPRECATED, use buildPublishDockerImage() method')
    buildPublishDockerImage(appName, buildVersion, extraPath)
}

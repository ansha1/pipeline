def call(String deployEnvironment, String assetDir, String version, String packageName) {
    log.warning('DEPRECATED: Use nexus.uploadStaticAssets() method.')
    nexus.uploadStaticAssets(deployEnvironment, assetDir, version, packageName)
}

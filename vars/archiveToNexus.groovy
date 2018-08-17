def call(String deployEnvironment, String assetDir, String version, String packageName) {
    log.deprecated('Use nexus.uploadStaticAssets() method.')
    nexus.uploadStaticAssets(deployEnvironment, assetDir, version, packageName)
}

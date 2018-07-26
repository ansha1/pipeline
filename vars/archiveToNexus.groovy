def call(String deployEnvironment, String assetDir, String version, String packageName) {
    log.warning('DEPRECATED: Use nexus.pushStaticAssets() method.')
    nexus.pushStaticAssets(deployEnvironment, assetDir, version, packageName)
}

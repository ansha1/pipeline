package com.nextiva.tools.build

import com.nextiva.config.Config
import com.nextiva.config.DeploymentType

class Make extends BuildTool {

    Config config = Config.getInstance()
    String packageName
    Map <String, String> assetDirs = [:]

    Make(Map buildToolConfig) {
        super(buildToolConfig)
        packageName = buildToolConfig.get("packageName","${config.appName}.tgz")

        if (buildCommands == null) {
            buildCommands = "make clean build"
        }
        if (unitTestCommands == null) {
            unitTestCommands = "make test"
        }
        if (publishCommands == null) {
            publishCommands = {
                // execute("make package PACKAGE_NAME=${config.appName}.tgz")
                String repositoryType = "dev"
                if (config.deploymentType == DeploymentType.RELEASE) {
                    repositoryType = "production"
                }
                def nexus = config.script.nexus
                assetDirs.each {
                    String assetName = it.key
                    String assetDir = it.value
                    nexus.uploadStaticAssets(repositoryType, assetDir, config.version, assetName)
                }
            }
        }
    }

    @Override
    String getVersion() {
        return execute("make get-version")
    }

    @Override
    void setVersion(String version) {
        execute("make set-version NEW_VERSION=${config.version}")
    }

    @Override
    void sonarScan() {
        // TODO
    }

    @Override
    void securityScan() {
        // TODO
    }

    @Override
    Boolean isArtifactAvailableInRepo() {
        return config.script.nexus.isAssetsPackageExists(config.appName, config.version)
    }

}

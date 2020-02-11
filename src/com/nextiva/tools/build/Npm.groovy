package com.nextiva.tools.build

import com.nextiva.config.DeploymentType

import static com.nextiva.config.Config.instance as config
import static com.nextiva.SharedJobsStaticVars.NPM_NEXTIVA_REGISTRY

class Npm extends BuildTool {

    Map <String, String> assetDirs = [:]

    def defaultCommands = [
            unitTest: """\
                npm run test
                npm run lint
            """.stripIndent(),
            publish : {
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
            },
            build   : "npm ci --registry='$NPM_NEXTIVA_REGISTRY'"
    ]

    File packageJsonFile

    Npm(Map toolConfiguration) {
        super(toolConfiguration)
        assetDirs = toolConfiguration.assetDirs
        if (unitTestCommands == null) {
            unitTestCommands = defaultCommands.unitTest
        }
        if (publishCommands == null) {
            publishCommands = defaultCommands.publish
        }
        if (buildCommands == null) {
            buildCommands = defaultCommands.build
        }
        packageJsonFile = new File(toolConfiguration.get("packageJson", "./package.json"))
    }

    @Override
    void setVersion(String version) {
        execute("npm version ${version} --no-git-tag-version --allow-same-version")
    }

    @Override
    String getVersion() {
        execute {
            def packageJson = config.script.readJSON file: packageJsonFile.path
            return packageJson.version
        }
    }

    @Override
    void sonarScan() {
        logger.info("this step should be implemented")
    }

    @Override
    void securityScan() {
        logger.info("this step should be implemented")
    }

    @Override
    Boolean isArtifactAvailableInRepo() {
        config.script.nexus.isAssetsPackageExists(appName, config.version)
    }
}

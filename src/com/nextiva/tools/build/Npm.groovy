package com.nextiva.tools.build

import com.nextiva.config.Global
import static com.nextiva.SharedJobsStaticVars.NPM_NEXTIVA_REGISTRY
import static com.nextiva.SharedJobsStaticVars.NPM_NEXTIVA_PRIVATE_REGISTRY

class Npm extends BuildTool {

    def defaultCommands = [
            unitTest: """\
                npm run test
                npm run lint
            """.stripIndent(),
            publish : {
                script.withCredentials([string(credentialsId: 'jenkins-npm-auth', variable: 'NPM_CONFIG__AUTH')]) {
                    script.sh "npm publish -g --scope '@nextiva' --registry='$NPM_NEXTIVA_PRIVATE_REGISTRY'"
                }
            },
            build   : "npm ci --registry='$NPM_NEXTIVA_REGISTRY'"
    ]

    Npm(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
        if (unitTestCommands == null) {
            unitTestCommands = defaultCommands.unitTest
        }
        if (publishCommands == null) {
            publishCommands = defaultCommands.publish
        }
        if (buildCommands == null) {
            buildCommands = defaultCommands.build
        }
    }

    @Override
    void setVersion(String version) {
        execute("npm version ${version} --no-git-tag-version --allow-same-version")
    }

    @Override
    String getVersion() {
        execute {
            def packageJson = script.readJSON file: "package.json"
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
        script.nexus.isAssetsPackageExists(appName, Global.instance.globalVersion)
    }
}

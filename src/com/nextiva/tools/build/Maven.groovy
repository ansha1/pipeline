package com.nextiva.tools.build

import static com.nextiva.config.Config.instance as config

import static com.nextiva.SharedJobsStaticVars.MAVEN_RELEASE_REPO
import static com.nextiva.SharedJobsStaticVars.SONAR_QUBE_ENV
import hudson.AbortException

class Maven extends BuildTool {


    def defaultCommands = [
            build   : "mvn clean package -U --batch-mode",
            unitTest: "",
            publish : "mvn deploy --batch-mode -DskipTests=true -Dmaven.test.skip=true"
    ]

    Maven(Map toolConfiguration) {
        super(toolConfiguration)
        if (buildCommands == null) {
            buildCommands = defaultCommands.build
        }
        if (unitTestCommands == null) {
            unitTestCommands = defaultCommands.unitTest
        }
        if (publishCommands == null) {
            publishCommands = defaultCommands.publish
        }
    }

    @Override
    void sonarScan() {
        execute {
            //TODO: add sonar implementation
            logger.info("this step should be implemented")
//            script.withSonarQubeEnv(SONAR_QUBE_ENV) {
//                sh 'mvn sonar:sonar'
//            }
        }
    }

    @Override
    void securityScan() {
        //TODO: add securityScan implementation
        logger.info("this step should be implemented")
    }

    @Override
    void setVersion(String version) {
        execute("mvn versions:set -DnewVersion=${version} -DgenerateBackupPoms=false")
    }

    @Override
    String getVersion() {
        execute {
            def rootPom = config.script.readMavenPom file: "pom.xml"
            String version = rootPom.version
            if (config.branchName ==~ /^(dev|develop)$/ && version != null && !version.contains("SNAPSHOT")) {
                throw new AbortException("Java projects built from the develop/dev branch require a version number that contains SNAPSHOT")
            }
            return version
        }
    }

    @Override
    Boolean isArtifactAvailableInRepo() {
        return config.script.nexus.isJavaPackageExists(appName, config.version, MAVEN_RELEASE_REPO)
    }
}

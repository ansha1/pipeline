package com.nextiva.tools.build

import static com.nextiva.SharedJobsStaticVars.SONAR_QUBE_ENV
import static com.nextiva.utils.Utils.getGlobalVersion
import static com.nextiva.utils.Utils.setGlobalVersion
import hudson.AbortException

class Maven extends BuildTool {

    def publishCommands = "mvn deploy -U --batch-mode -DskipTests"

    Maven(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
    }

    @Override
    void sonarScan() {
        execute {
            //TODO: add sonar implementation
            log.info("this step should be implemented")
//            script.withSonarQubeEnv(SONAR_QUBE_ENV) {
//                sh 'mvn sonar:sonar'
//            }
        }
    }

    @Override
    void securityScan() {
        //TODO: add securityScan implementation
        log.info("this step should be implemented")
    }

    @Override
    void setVersion(String version) {
        execute("mvn versions:set -DnewVersion=${version} -DgenerateBackupPoms=false")
    }

    @Override
    String getVersion() {
        execute {
            def rootPom = script.readMavenPom file: "pom.xml"
            String version = rootPom.version
            if (configuration.get("branch") ==~ /^(dev|develop)$/ && version != null && !version.contains("SNAPSHOT")) {
                throw new AbortException("Java projects built from the develop/dev branch require a version number that contains SNAPSHOT")
            }
            return version
        }
    }

    @Override
    Boolean isArtifactAvailableInRepo() {
        return null
    }
}

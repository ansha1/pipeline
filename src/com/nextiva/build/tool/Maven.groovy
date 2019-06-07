package com.nextiva.build.tool

import static com.nextiva.SharedJobsStaticVars.SONAR_QUBE_ENV

class Maven extends BuildTool {

    Maven(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
    }

    @Override
    void sonarScan() {
        execute {
            script.withSonarQubeEnv(SONAR_QUBE_ENV) {
                sh 'mvn sonar:sonar'
            }
        }
    }

    @Override
    void securityScan() {

    }

    @Override
    void setVersion(String version) {
        execute("mvn versions:set -DnewVersion=${version} -DgenerateBackupPoms=false")
    }

    @Override
    String getVersion() {
        execute {
            def rootPom = script.readMavenPom file: "pom.xml"
            def version = rootPom.version
            if (script.env.BRANCH_NAME ==~ /^(dev|develop)$/ && version != null && !version.contains("SNAPSHOT")) {
                script.error 'Java projects built from the develop/dev branch require a version number that contains SNAPSHOT'
            }
            return version
        }
    }


    @Override
    publish() {
        execute(toolConfiguration.get("publishCommands", "mvn deploy -U --batch-mode -DskipTests"))
    }

    @Override
    Boolean isArtifactAvailableInRepo() {
        return null
    }
}

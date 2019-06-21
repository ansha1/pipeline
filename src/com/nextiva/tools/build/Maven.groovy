package com.nextiva.tools.build

import com.nextiva.SharedJobsStaticVars
import com.nextiva.utils.Logger

class Maven extends BuildTool {
    Logger log = new Logger(this)

    Maven(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
    }

    @Override
    void sonarScan() {
        execute {
            script.withSonarQubeEnv(SharedJobsStaticVars.SONAR_QUBE_ENV) {
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
    void publish() {
        execute(toolConfiguration.get("publishCommands", "mvn deploy -U --batch-mode -DskipTests"))
    }

    @Override
    Boolean isArtifactAvailableInRepo() {
        return null
    }
}

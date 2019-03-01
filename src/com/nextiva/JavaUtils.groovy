package com.nextiva

import static com.nextiva.SharedJobsStaticVars.*
import groovy.transform.Field


@Field String modulesPropertiesField
@Field String pathToSrc = '.'


String getVersion() {
    dir(pathToSrc) {
        def rootPom = readMavenPom file: "pom.xml"
        def version = rootPom.version
        if (env.BRANCH_NAME ==~ /^(dev|develop)$/ && version != null && !version.contains("SNAPSHOT")){
            error 'Java projects built from the develop/dev branch require a version number that contains SNAPSHOT'
        }
        return version
    }
}


void setVersion(String version) {
    dir(pathToSrc) {
        sh "mvn versions:set -DnewVersion=${version} -DgenerateBackupPoms=false"
    }
}


String createReleaseVersion(String version) {
    def releaseVersion = version.replaceAll("-SNAPSHOT", "")
    return releaseVersion
}


def runSonarScanner(String projectVersion) {
    scannerHome = tool SONAR_QUBE_SCANNER

    dir(pathToSrc) {
        withSonarQubeEnv(SONAR_QUBE_ENV) {
            sh 'mvn sonar:sonar'
        }
        /*timeout(time: 30, unit: 'MINUTES') {
            def qg = waitForQualityGate()
            if (qg.status != 'OK') {
                log.warning('Sonar Quality Gate failed')
                // currentBuild.rawBuild.result = Result.UNSTABLE
            }
        }*/
    }
}



void buildForVeracode(String appName, String buildVersion, String environment, Map args) {

    /*
       *  veracode does not support spring-boot applications, therefore we remove <executable>true</executable> from any
       *  pom.xml files that contain them and then do standard (non deploy) build in maven to prepare for submission to scan in veracode
       *
       */

    //sets modulesPropertiesField which we need in jobTemplate.groovy where it runs veracodeScan.groovy to know the components that need to be uploaded for scanning.
    getPropertiesForVeracode()
    //find the pomFile that contains <executable>true</executable>
    String pomFile = sh returnStdout: true, script: "find ./ -type f -name pom.xml | xargs grep \"<executable>true</executable>\" | cut -d \":\" -f 1"
    def pomLength = pomFile.size()
    if (pomLength > 0) { //will be greater than 0 if the file exists
        log.info("found spring-boot executable pom file: $pomFile")

        //remove the offending line from the pom file
        sh "sed -i -- \"s|<executable>true</executable>||\" $pomFile"
        //sanity check to make sure its actually removed, probably frivolous
        String pomFile2 = sh returnStdout: true, script: "find ./ -type f -name pom.xml | xargs grep \"<executable>true</executable>\" | cut -d \":\" -f 1"
        log.info("pom file verify: $pomFile2")
    }

    // build the artifact
    log.info("Build for veracode scanning")
    log.info("APP_NAME: ${appName}")
    log.info("BUILD_VERSION: ${buildVersion}")
    log.info("ENV: ${environment}")

    def verracodeBuildCommands = args.get('verracodeBuildCommands', 'mvn clean install -U --batch-mode -DskipTests')
    dir(pathToSrc) {
        try {
            sh "${verracodeBuildCommands}"
        } catch (e) {
            error("buildForVeracode fail ${e}")
        }

    }
}

List getPropertiesForVeracode() {

    /*
   *  method getPropertiesForVeracode() is used to collect the maven modules properties from a local source
   *  It returns a list of Maps with module's groupId, artifactId, artifactVersion, packaging and finalName
   */

    log.info("get Java artifacts properties: groupId, version, artifactId, packaging and finalName")
    List artifactsListProperties = []
    dir(pathToSrc) {

       // sh "mvn -q clean install -DskipTests=true"

        def artifactsProperties = sh returnStdout: true, script: """
                                                                    mvn -q -Dexec.executable=\'echo\' -Dexec.args=\'\${project.groupId} \${project.artifactId} \${project.version} \${project.packaging} \${project.build.finalName}\' exec:exec -U
                                                                 """
        log.debug("Received artifact properties: $artifactsProperties")
        modulesPropertiesField = artifactsProperties
        artifactsProperties.split('\n').each {
            def propertiesList = it.split()
            log.debug("Received propertiesList: $propertiesList")
            if (propertiesList.size() >= 4) {
                artifactsListProperties << ['groupId': propertiesList[0], 'artifactVersion': propertiesList[2], 'artifactId': propertiesList[1], 'packaging': propertiesList[3], 'finalName': propertiesList[4]]

            }
        }
    }

    log.debug("method getPropertiesForVeracode() returned: ${artifactsListProperties}")
    return artifactsListProperties

}


List getModulesProperties() {
    /*
    *  method getModulesProperties() is used to collect the maven modules properties from a local source
    *  It returns a list of Maps with module's groupId, artifactId, artifactVersion, packaging
    */

    log.info("get Java artifacts properties: groupId, version, artifactId, packaging")
    List artifactsListProperties = []
    dir(pathToSrc) {

        sh "mvn -q clean install -DskipTests=true"

        def artifactsProperties = sh returnStdout: true, script: """
                                                                    mvn -q -Dexec.executable=\'echo\' -Dexec.args=\'\${project.groupId} \${project.artifactId} \${project.version} \${project.packaging}\' exec:exec -U
                                                                 """
        log.debug("Received artifact properties: $artifactsProperties")
        //line below is only needed for veracode scans, if it overwrites the properties set by getPropertiesForVeracode, then veracode java scans wont work.
        //modulesPropertiesField = artifactsProperties
        artifactsProperties.split('\n').each {
            def propertiesList = it.split()
            log.debug("Received propertiesList: $propertiesList")
            if (propertiesList.size() >= 3) { //sanity check, without this our tests fail
                artifactsListProperties << ['groupId': propertiesList[0], 'artifactVersion': propertiesList[2], 'artifactId': propertiesList[1], 'packaging': propertiesList[3]]

            }
        }
    }

    log.debug("method getModulesProperties() returned: ${artifactsListProperties}")
    return artifactsListProperties
}


Boolean isMavenArtifactVersionsEqual(List artifactsListProperties) {
    return (new HashSet(artifactsListProperties.collect { it.get('artifactVersion') }).size() == 1)
}


Boolean verifyPackageInNexus(String packageName, String packageVersion, String deployEnvironment) {
    Integer counter = 0
    List artifactsInNexus = []
    List mavenArtifactsProperties = []

    try {
        mavenArtifactsProperties = getModulesProperties()
        mavenArtifactsProperties.each { artifact ->
            // if packageVersion is the same that is currently set in pom.xml we are calculating sub modules
            // properties with getModulesProperties() and do Nexus verification for all found artifacts with their local versions.
            // Otherwise the version that was explicitly passed to the method will be used instead of the local one - used for autoincrement
            if (!getVersion().equals(packageVersion)) {
                artifact.artifactVersion = packageVersion
            }
            if (nexus.isJavaArtifactExists(artifact.groupId, artifact.artifactId, artifact.artifactVersion, artifact.packaging)) {
                counter++
                artifactsInNexus << artifact
            }
        }
    } catch (e) {
        log.error("There was a problem with mvn artifacts version validation " + e)
        return false
    }

    if (counter == mavenArtifactsProperties.size() && isMavenArtifactVersionsEqual(mavenArtifactsProperties)) {
        // returning true only in case when all artifacts exist in Nexus and have the same version
        return true
    } else if (counter == 0) {
        // returning false only when none of the artifacts exists in Nexus
        return false
    } else {
        log.error("The following artifact already exists in Nexus and we can't auto increment a version for them: ${artifactsInNexus}")
        currentBuild.rawBuild.result = Result.ABORTED
        throw new hudson.AbortException("\nCan't apply autoincrement method. Please review versions in used submodules pom.xml" +
                "\nThe used versions should be identical for all submodules or you need manually set the versions that don't exist in Nexus")
    }
}


void runTests(Map args) {
    log.info("Start unit tests Java")

    def testCommands = args.get('testCommands', 'mvn --batch-mode clean install -U jacoco:report && mvn checkstyle:checkstyle')
    dir(pathToSrc) {
        try {
            sh testCommands
        } catch (e) {
            error("Unit test fail ${e}")
        } finally {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/target/checkstyle-result.xml', unHealthy: ''
        }
    }
}


void buildPublish(String appName, String buildVersion, String environment, Map args) {
    log.info("Build and publish Java application.")
    log.info("APP_NAME: ${appName}")
    log.info("BUILD_VERSION: ${buildVersion}")
    log.info("ENV: ${environment}")
    def buildCommands = args.get('buildCommands', 'mvn deploy -U --batch-mode -DskipTests')
    dir(pathToSrc) {
        try {
            sh "${buildCommands}"
        } catch (e) {
            error("buildPublish fail ${e}")
        }
    }

}

void buildRelease(String appName, String buildVersion, String environment, Map args, String deployVersion) {
    log.info("Build and Release Java application.")
    log.info("APP_NAME: ${appName}")
    log.info("BUILD_VERSION: ${buildVersion}")
    log.info("DEPLOY_VERSION: ${deployVersion}")
    log.info("ENV: ${environment}")
    def deployVersionArg = deployVersion.isEmpty() ? "" : "-DreleaseVersion=${deployVersion}"
    def releaseCommand = args.get("releaseCommands", "mvn --batch-mode release:prepare -DskipTests ${deployVersionArg} && mvn --batch-mode release:perform -DskipTests")
    dir(pathToSrc) {
        try {
            sh "${releaseCommand}"
        } catch (e) {
            error("buildRelease fail ${e}")
        }
    }
}
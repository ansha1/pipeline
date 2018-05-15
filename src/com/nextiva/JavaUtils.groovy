package com.nextiva

import static com.nextiva.SharedJobsStaticVars.getSONAR_QUBE_ENV
import static com.nextiva.SharedJobsStaticVars.getSONAR_QUBE_SCANNER

//class JavaUtils implements Utils {

    final String pathToSrc

//    JavaUtils(String pathToSrc) {
//        this.pathToSrc = pathToSrc
//    }
//
//    JavaUtils() {
//        this.pathToSrc = '.'
//    }

    @Override
    String getVersion() {
        def rootPom = readMavenPom file: "${pathToSrc}/pom.xml"
        return rootPom.version
    }

    @Override
    void setVersion(String version) {

        sh "cd ${pathToSrc} && mvn versions:set -DnewVersion=${version} -DgenerateBackupPoms=false"

    }

    @Override
    String createReleaseVersion(String version) {
        def releaseVersion = version.replaceAll("-SNAPSHOT", "")
        return releaseVersion
    }

    @Override
    def runSonarScanner(String projectVersion) {
        scannerHome = tool SONAR_QUBE_SCANNER
        withSonarQubeEnv(SONAR_QUBE_ENV) {
            sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectVersion=${projectVersion}"
        }
    }

    @Override
    void runTests() {
        print("\n\n Start unit tests Java \n\n")
        dir(pathToSrc) {
            try {
                sh 'mvn clean install jacoco:report'
                sh 'mvn checkstyle:checkstyle'
            } catch (e) {
                error("Unit test fail ${e}")
            } finally {
                junit '**/target/surefire-reports/*.xml'
                checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/target/checkstyle-result.xml', unHealthy: ''
            }
        }
    }

    @Override
    void buildPublish() {
        print("\n\n build and publish Java \n\n")
        dir(pathToSrc) {
            try {
                sh 'mvn deploy --batch-mode -DskipTests'
            } catch (e) {
                error("buildPublish  fail ${e}")

            }
        }
    }

    @Override
    void setBuildVersion(String userDefinedBuildVersion) {

        if (!userDefinedBuildVersion) {
            version = getVersion()
            DEPLOY_ONLY = false
            echo('===========================')
            echo('Source Defined Version = ' + version)
        } else {
            version = userDefinedBuildVersion.trim()
            DEPLOY_ONLY = true
            echo('===========================')
            echo('User Defined Version = ' + version)
        }

        ANSIBLE_EXTRA_VARS = ['application_version': version,
                              'maven_repo'         : version.contains('SNAPSHOT') ? 'snapshots' : 'releases']

        BUILD_VERSION = version - "SNAPSHOT" + "-" + env.BUILD_ID
        echo('===============================')
        echo('POM VERSION ' + version)
        echo('BUILD_VERSION ' + BUILD_VERSION)
        echo('===============================')
        print('DEPLOY_ONLY:  ' + DEPLOY_ONLY)
        echo('===============================')
    }
//}
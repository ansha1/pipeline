package com.nextiva

import static com.nextiva.SharedJobsStaticVars.getSONAR_QUBE_ENV
import static com.nextiva.SharedJobsStaticVars.getSONAR_QUBE_SCANNER

interface Utils {

    String getVersion()
    void setVersion(String version)
    String createReleaseVersion(String version)
    def runSonarScanner(String projectVersion)
    void runTests()
    void buildPublish()
    void setBuildVersion(String userDefinedBuildVersion)
}
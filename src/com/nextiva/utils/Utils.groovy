package com.nextiva.utils

import com.cloudbees.groovy.cps.NonCPS
import jenkins.model.Jenkins

class Utils {

    static String shWithOutput(script, String command) {
        return script.sh(
                script: command + " 2>&1",
                returnStdout: true
        ).trim()
    }

    static def shOrClosure(script, def command) {
        def result
        if (command instanceof Closure) {
            result = command()
        } else {
            result = shWithOutput(script, command)
        }
        return result
    }

    /**
     * Returns the id of the build, which consists of the job name,
     * build number.
     * By convention, the names of Kubernetes resources should be up to maximum length of 253 characters and consist of
     * lower case alphanumeric characters, -, and ., but certain resources have more specific restrictions.
     * @param jobName usually env.JOB_NAME
     * @param buildNum usually env.BUILD_NUMBER
     * @return
     */
    static String buildID(String jobName, String buildNumber) {
        String name = jobName.replaceAll('[^a-zA-Z\\d]', '-')
                .replaceFirst('^-+', '')
                .toLowerCase().take(200)
        return "$name-$buildNumber"
    }

    @NonCPS
    def getCurrentBuildInstance(script) {
        return script.currentBuild
    }

    @NonCPS
    def getRawBuild(script) {
        return getCurrentBuildInstance(script).rawBuild
    }

    def isJobStartedByTimer(Script script) {
        return isJobStartedByCause(script, hudson.triggers.TimerTrigger.TimerTriggerCause.class)
    }

    def isJobStartedByUser(Script script) {
        return isJobStartedByCause(script, hudson.model.Cause.UserIdCause.class)
    }

    @NonCPS
    def isJobStartedByCause(Script script, Class cause) {
        def startedByGivenCause = false
        def detectedCause = getRawBuild(script).getCause(cause)
        if (detectedCause != null) {
            startedByGivenCause = true
            echo "Found build cause ${detectedCause}"
        }
        return startedByGivenCause
    }

    static String collectionToString(def collection) {
        //This method needs to reduce output in Jenkins console when print object entryset
        String toString = ""
        collection.each { k, v ->
            toString += "[$k]: $v\n"
        }
        return toString
    }

}

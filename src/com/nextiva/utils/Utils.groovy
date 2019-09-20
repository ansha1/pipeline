package com.nextiva.utils

import com.cloudbees.groovy.cps.NonCPS
import com.nextiva.config.Global
import hudson.AbortException
import jenkins.model.Jenkins

class Utils {

    /**
     * Executes command in a
     * <a href="https://jenkins.io/doc/pipeline/steps/workflow-durable-task-step/#sh-shell-script">Jenkins sh</a> step
     * @param script Jenkins script
     * @param command shell script to execute
     * @return command output
     */
    static String shWithOutput(script, String command) {
        return script.sh(
                script: command + " 2>&1",
                returnStdout: true
        ).trim()
    }

    /**
     * Depending on command type, either run it as a shell command or as a Closure
     * @param script Jenkins script
     * @param command command closure to execute
     * @return shell or closure output
     */
    static def shOrClosure(script, def command) {
        def result
        if (command instanceof Closure) {
            command.delegate = this
            result = command()
        } else {
            result = shWithOutput(script, command)
        }
        return result
    }

    /**
     * Returns the id of the build, which consists of the job name,
     * build number.
     * By convention, the names of Kubernetes namespace should be up to maximum length of 63 characters and consist of
     * lower case alphanumeric characters, -, and ., but certain resources have more specific restrictions.
     * @param jobName usually env.JOB_NAME
     * @param buildNum usually env.BUILD_NUMBER
     * @return
     */
    static String buildID(String jobName, String buildNumber) {
        String name = jobName.replaceAll('[^a-zA-Z\\d]', '-')
                .replaceFirst('^-+', '')
                .toLowerCase().take(63 - buildNumber.length() - 1) // 1 subtracted to save space for hyphen character
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

    /**
     * Gets a single property value from a property file
     * @param script Jenkins script
     * @param propertyFilePath path to the property file
     * @param propertyName name of the property to retrieve
     * @return property value
     */
    static def getPropertyFromFile(Script script, String propertyFilePath, String propertyName) {
        def property
        if (script.fileExists(propertyFilePath)) {
            def buildProperties = script.readProperties file: propertyFilePath
            property = buildProperties.get(propertyName)
        } else {
            throw new AbortException("File ${propertyFilePath} not found.")
        }
        return property
    }

    /**
     * Stores a single property value into a property file. Overwrites existing property.
     * @param script Jenkins script
     * @param propertyFilePath path to the property file
     * @param propertyName name of the property to retrieve
     * @param value property value to store
     */
    static void setPropertyToFile(Script script, String propertyFilePath, String propertyName, String value) {
        String propsToWrite = ''
        if (script.fileExists(propertyFilePath)) {
            def buildProperties = script.readProperties file: propertyFilePath
            buildProperties[propertyName] = value
            buildProperties.each {
                propsToWrite = propsToWrite + it.toString() + '\n'
            }
            script.writeFile file: propertyFilePath, text: propsToWrite
        } else {
            throw new AbortException("File ${propertyFilePath} not found.")
        }
    }

    /**
     * Gets application version from Global singleton
     * @return application version
     */
    static String getGlobalVersion() {
        Global global = getGlobal()
        if (global == null) {
            throw new AbortException("Configuration is not initialized, aborting...")
        }
        return global.getGlobalVersion()
    }

    /* Below behaves unstable and returns  com.nextiva.config.Global@5a88b120 as a String instead of appName value
    (e.g. "myapp")
    @NonCPS
    static String getGlobalAppName() {
        Global global = getGlobal()
        if (global == null) {
            throw new AbortException("Configuration is not initialized, aborting...")
        }
        return global.getAppName()
    }
    */


    /**
     * Stores application version in Global singleton
     * @param version version to save
     */
    static void setGlobalVersion(String version) {
        Global global = getGlobal()
        if (global == null) {
            throw new AbortException("Configuration is not initialized, aborting...")
        }
        global.setGlobalVersion(version)
    }

    /**
     * Gets Global singleton
     * @return Global singleton
     */
    static Global getGlobal() {
        return Global.getInstance()
    }
}

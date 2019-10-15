package com.nextiva.tools.build

import hudson.AbortException

import java.util.regex.Pattern

import static com.nextiva.SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
import static com.nextiva.SharedJobsStaticVars.NEXTIVA_DOCKER_REGISTRY
import static com.nextiva.SharedJobsStaticVars.NEXTIVA_DOCKER_REGISTRY_CREDENTIALS_ID
import static com.nextiva.SharedJobsStaticVars.NEXTIVA_DOCKER_REGISTRY_URL
import static com.nextiva.utils.Utils.getPropertyFromFile
import static com.nextiva.utils.Utils.setPropertyToFile
import static com.nextiva.utils.Utils.shWithOutput
import static com.nextiva.config.Global.instance as global

class Docker extends BuildTool {
    String dockerfileName
    String buildLocation

    Docker(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
        this.dockerfileName = toolConfiguration.get("dockerfileName")
        this.buildLocation = toolConfiguration.get("buildLocation")
    }

    /**
     * Running the same as a closure passed to publishCommand fails because of numerous CPS issues
     */
    @Override
    void publish() {
        if (publishArtifact == false) {
            logger.info("Skipping publish, because publishArtifact is set to false")
            return
        }
        logger.debug("Checking if image should be tagged by 'latest'")
        Boolean tagLatest = isTagLatest()
        logger.debug("Tag image with 'latest'? $tagLatest")
        logger.debug("Going to run buildPublish")
        String version = getVersion()
        logger.debug("Got version: $version")
        execute {
            buildPublish(script, NEXTIVA_DOCKER_REGISTRY_URL, NEXTIVA_DOCKER_REGISTRY_CREDENTIALS_ID, global.appName,
                    version, dockerfileName, buildLocation, tagLatest)
        }
        logger.debug("buildPublish completed")
    }

    @Override
    String getVersion() {
        script.dir(pathToSrc) {
            String version = null
            try {
                version = getPropertyFromFile(script, BUILD_PROPERTIES_FILENAME, "version")
            } catch (e) {
                logger.debug("$BUILD_PROPERTIES_FILENAME not found for the Docker build tool, e:", e)
            }
            if (version == null) {
                logger.debug("Try to get version from GLOBAL version")
                version = global.globalVersion
            }
            if (version == null) {
                throw new AbortException("Version for Docker is undefined, please define it in $BUILD_PROPERTIES_FILENAME or by another build tool via GLOBAL version")
            }
            return version
        }
    }

    @Override
    void setVersion(String version) {
        execute {
            try {
                setPropertyToFile(script, BUILD_PROPERTIES_FILENAME, "version", version)
            } catch (e) {
                logger.warn("File ${BUILD_PROPERTIES_FILENAME} not found, can't set version e:", e)
            }
        }
    }

    @Override
    void sonarScan() {
        logger.debug("sonarScan is not exist for this type of Build tool")
    }

    @Override
    void securityScan() {
        //TODO: we should implement security scan for docker containers
        logger.warn("we should implement security scan for docker containers")
    }

    @Override
    Boolean isArtifactAvailableInRepo() {
        execute {
            logger.debug("try to find the docker image ${appName} with version:${getVersion()} in the Nexus registry")
            return script.nexus.isDockerPackageExists(appName, getVersion())
        }
    }

    //For more info see https://jenkins.nextiva.xyz/jenkins/pipeline-syntax/globals#docker
    def buildPublish(Script script, String registry, String registryCredentials, String appName, String version, String dockerFilePath = "Dockerfile", String buildLocation = ".", Boolean tagLatest = false) {
        script.docker.withRegistry(registry, registryCredentials) {
            logger.debug("Building image ")
            def image = script.docker.build("$appName:$version", "-f $dockerFilePath --build-arg build_version=${version} ${buildLocation}")
            logger.debug("Pushing image $version")
            image.push()
            if (tagLatest) {
                logger.debug("Pushing image with 'latest' tag")
                image.push("latest")
                logger.debug("Removing image with 'latest'")
                String output = shWithOutput(script, "docker rmi ${registry.replaceFirst(/^https?:\/\//, '')}/${appName}:latest")
                logger.debug("$output")
            }
            logger.debug("Removing image $version")
            String output = shWithOutput(script, "docker rmi ${image.id} ${registry.replaceFirst(/^https?:\/\//, '')}/${image.id}")
            logger.debug("$output")
        }
    }

    Boolean isTagLatest() {
        logger.trace("Getting branch name from Global")
        String branchName = global.getBranchName()
        logger.trace("branchName: $branchName")
        logger.trace("Getting branching model from Global")
        String branchingModel = global.getBranchingModel()
        logger.trace("branchModel:  $branchingModel")
        Map chooser = ["gitflow"   : /^(dev|develop)$/,
                       "trunkbased": /^master$/]
        logger.trace("chooser: $chooser")

        logger.trace("Compiling regex pattern: ${chooser.get(branchingModel)}")
        Pattern branchPattern = Pattern.compile(chooser.get(branchingModel))

        logger.trace("$branchName ==~ $branchPattern: ")
        Boolean result = branchName ==~ branchPattern
        logger.trace("Result is $result")
        return result
    }
}


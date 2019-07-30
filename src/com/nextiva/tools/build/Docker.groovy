package com.nextiva.tools.build

import com.nextiva.config.Global
import hudson.AbortException

import java.util.regex.Pattern

import static com.nextiva.SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
import static com.nextiva.SharedJobsStaticVars.NEXTIVA_DOCKER_REGISTRY
import static com.nextiva.SharedJobsStaticVars.NEXTIVA_DOCKER_REGISTRY_CREDENTIALS_ID
import static com.nextiva.utils.Utils.getGlobal
import static com.nextiva.utils.Utils.getPropertyFromFile
import static com.nextiva.utils.Utils.getGlobalVersion
import static com.nextiva.utils.Utils.setPropertyToFile
import static com.nextiva.utils.Utils.shWithOutput

class Docker extends BuildTool {
    def publishCommands = {
        Boolean tagLatest = isTagLatest()
        buildPublish(script, NEXTIVA_DOCKER_REGISTRY, NEXTIVA_DOCKER_REGISTRY_CREDENTIALS_ID, appName, getVersion(), "Dockerfile", ".", tagLatest)
    }

    Docker(Script script, Map toolConfiguration) {
        super(script, toolConfiguration)
    }

    @Override
    String getVersion() {
        execute {
            String version = null
            try {
                version = getPropertyFromFile(script, BUILD_PROPERTIES_FILENAME, "version")
            } catch (e) {
                log.debug("$BUILD_PROPERTIES_FILENAME not found for the Docker build tool, e:", e)
            }
            if (version == null) {
                log.debug("Try to get version from GLOBAL version")
                version = getGlobalVersion()
            }
            if (version == null) {
                throw new AbortException("Version for Docker is undefined, please define it in $BUILD_PROPERTIES_FILENAME or by another build tool via GLOBAL version")
            }
        }
    }

    @Override
    void setVersion(String version) {
        execute {
            try {
                setPropertyToFile(script, BUILD_PROPERTIES_FILENAME, "version", version)
            } catch (e) {
                log.warn("File ${BUILD_PROPERTIES_FILENAME} not found, can't set version e:", e)
            }
        }
    }

    @Override
    void sonarScan() {
        log.debug("sonarScan is not exist for this type of Build tool")
    }

    @Override
    void securityScan() {
        //TODO: we should implement security scan for docker containers
        log.warn("we should implement security scan for docker containers")
    }

    @Override
    Boolean isArtifactAvailableInRepo() {
        execute {
            log.debug("try to find the docker image ${appName} with version:${getVersion()} in the Nexus registry")
            return script.nexus.isDockerPackageExists(appName, getVersion())
        }
    }

    //For more info see https://jenkins.nextiva.xyz/jenkins/pipeline-syntax/globals#docker
    def buildPublish(Script script, String registry, String registryCredentials, String appName, String version, String dockerFilePath = "Dockerfile", String buildLocation = ".", Boolean tagLatest = false) {
        script.docker.withRegistry(registry, registryCredentials) {
            def image = script.docker.build("$appName:$version", "-f $dockerFilePath --build-arg build_version=${version} ${buildLocation}")
            image.push()
            if (tagLatest) {
                image.push("latest")
                String output = shWithOutput(script, "docker rmi ${registry.replaceFirst(/^https?:\/\//, '')}/${appName}:latest")
                log.debug("$output")
            }
            String output = shWithOutput(script, "docker rmi ${image.id} ${registry.replaceFirst(/^https?:\/\//, '')}/${image.id}")
            log.debug("$output")
        }
    }

    static Boolean isTagLatest() {
        Global global = getGlobal()
        String branchName = global.getBranchName()
        String branchingModel = global.getBranchingModel()
        Map chooser = ["gitflow"   : /^(dev|develop)$/,
                       "trunkbased": "master"]

        Pattern branchPattern =Pattern.compile(chooser.get(branchingModel))
        return branchName ==~ branchPattern
    }
}


package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool
import hudson.AbortException

import static com.nextiva.utils.Utils.setGlobalVersion

/**
 * Sets up global project version value based on the first non-empty version value from the project's build tool
 */
class ConfigureProjectVersion extends Stage {
    ConfigureProjectVersion(Script script, Map configuration) {
        super(script, configuration)
    }

    @Override
    def stageBody() {
        String version = ''

        Map build = configuration.get("build")
        build.find { toolName, toolConfiguration ->
            log.debug("Getting version from $toolName")
            BuildTool tool = toolConfiguration.get("instance")
            version = tool.getVersion()
            log.debug("Found verison is $version")
            return isValidVersion(version)
        }

        if (!isValidVersion(version)) {
            throw new AbortException('Unable to determine application version with the build tools you have defined.')
        }
        log.debug("Setting global version to $version")
        setGlobalVersion(version)
    }

    static boolean isValidVersion(String version) {
        return version != null && !version.trim().isEmpty()
    }

}

package com.nextiva.stages.stage

import com.nextiva.tools.build.BuildTool
import hudson.AbortException

import static com.nextiva.config.Config.instance as config

/**
 * Sets up global project version value based on the first non-empty version value from the project's build tool
 */
class ConfigureProjectVersion extends Stage {
    ConfigureProjectVersion() {
        super()
    }

    @Override
    def stageBody() {
        String version = ''

        List build = config.build
        build.find { toolConfiguration ->
            logger.debug("Getting version from ${toolConfiguration.name}")
            BuildTool tool = toolConfiguration.get("instance")
            version = tool.getVersion()
            logger.debug("Found verison is $version")
            return isValidVersion(version)
        }

        // TODO check that 'if (!version)' would work
        if (!isValidVersion(version)) {
            throw new AbortException('Unable to determine application version with the build tools you have defined.')
        }
        logger.debug("Setting global version to $version")
        config.version = version
    }

    static boolean isValidVersion(String version) {
        return version != null && !version.trim().isEmpty()
    }

}

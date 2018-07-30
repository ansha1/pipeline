package utils

import com.lesfurets.jenkins.unit.BasePipelineTest

/**
 * Provides assess to BasePipelineTest instance
 */
interface BasePipelineAccessor {
    BasePipelineTest getBasePipelineTest()
}

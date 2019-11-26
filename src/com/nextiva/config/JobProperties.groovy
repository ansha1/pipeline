package com.nextiva.config

import com.cloudbees.groovy.cps.NonCPS
import com.nextiva.utils.Logger

import java.util.regex.Pattern

class JobProperties {
    Script script
    List jobTriggers
    String buildDaysToKeep
    String buildNumToKeep
    String buildArtifactDaysToKeep
    String buildArtifactNumToKeep
    List paramlist
    Map auth


    JobProperties(Config configuration) {
        this.script = configuration.script
        this.jobTriggers = configuration.jobTriggers
        this.buildDaysToKeep = configuration.buildDaysToKeep
        this.buildNumToKeep = configuration.buildNumToKeep
        this.buildArtifactDaysToKeep = configuration.buildArtifactDaysToKeep
        this.buildArtifactNumToKeep = configuration.buildArtifactNumToKeep
        this.auth = configuration.auth
        this.paramlist = generateParamList(this.script, configuration.branchName, configuration.branchingModel,
                configuration.environmentsToDeploy.collect { return it.name })
    }

    @NonCPS
    private List generateParamList(Script script, String branchName, BranchingModel branchingModel, List<String> environmentsToDeploy) {
        // TODO if deployDst is empty, it makes Jenkins go oops. When built against master branch this parameter is
        //  deployDst is used somewhere if we are on master branch only for trunkbased
        List paramlist = []

        // TODO environmentsToDeploy is always empty here, becasue it is set later inside Config.configure()
        if (branchName ==~ /^(hotfix\/.+)$/) {
            //workaround to avoid deployment on qa env from hotfix branch on the first execution
            environmentsToDeploy.add(0, "")
        }
        environmentsToDeploy.add(0, "none")

        List jobParameters = [
                ["parameter"     : script.string(name: 'deployVersion', defaultValue: '',
                        description: 'If set, skip build stages and deploy specified artifact version'),
                 "branchingModel": [(GitFlow.class)   : BranchingModelRegexps.mainlineWithMaster,
                                    (TrunkBased.class): BranchingModelRegexps.master],
                ],
                ["parameter"     : script.choice(choices: environmentsToDeploy, description: 'Deployment destination',
                        name: 'deployDst'),
                 "branchingModel": [(GitFlow.class)   : BranchingModelRegexps.releaseOrHotfix,
                                    (TrunkBased.class): BranchingModelRegexps.master],
                ],
                ["parameter"     : script.choice(choices: ["DEBUG", "INFO", "DEBUG", "TRACE", "ALL"],
                        description: 'Pipeline Log level', name: 'JOB_LOG_LEVEL'),
                 "branchingModel": [(GitFlow.class)   : BranchingModelRegexps.any,
                                    (TrunkBased.class): BranchingModelRegexps.any]
                ]
        ]

        jobParameters.each {
            Pattern branchPattern = it.get("branchingModel").get(branchingModel.class)
            if (branchPattern.matcher(branchName).matches()) {
                paramlist.add(it.get("parameter"))
            }
        }

        return paramlist
    }


    Map toMap() {
        Map result = [:]
        result.put("jobTriggers", jobTriggers)
        result.put("buildDaysToKeepStr", buildDaysToKeep)
        result.put("buildNumToKeepStr", buildNumToKeep)
        result.put("buildArtifactDaysToKeepStr", buildArtifactDaysToKeep)
        result.put("buildArtifactNumToKeepStr", buildArtifactNumToKeep)
        result.put("paramlist", paramlist)
        result.put("auth", auth)

        return result
    }

    Map getParams() {
        def params = script.jobWithProperties(toMap())
        return params
    }

}

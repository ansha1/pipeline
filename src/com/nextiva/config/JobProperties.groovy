package com.nextiva.config

import com.cloudbees.groovy.cps.NonCPS

import java.util.regex.Pattern

class JobProperties {
    Script script
    List jobTriggers
    String buildDaysToKeepStr
    String buildNumToKeepStr
    String buildArtifactDaysToKeepStr
    String buildArtifactNumToKeepStr
    List paramlist
    Map auth

    JobProperties(Script script, Map configuration) {
        this.script = script
        this.jobTriggers = configuration.get("jobTriggers", [])
        this.buildDaysToKeepStr = configuration.get("buildDaysToKeepStr", "30")
        this.buildNumToKeepStr = configuration.get("buildNumToKeepStr", "30")
        this.buildArtifactDaysToKeepStr = configuration.get("buildArtifactDaysToKeepStr", "10")
        this.buildArtifactDaysToKeepStr = configuration.get("buildArtifactDaysToKeepStr", "10")
        this.auth = configuration.get("auth", [:])
        this.paramlist = generateParamList(script, configuration)
    }

    @NonCPS
    private List generateParamList(Script script, Map configuration) {
        List paramlist = []
        String branchName = configuration.get("branchName")
        String branchingModel = configuration.get("branchingModel")
        List<String> environmentsToDeploy = configuration.get("environmentsToDeploy", [[:]]).collect {
            return it.get("name", "")
        }
        if (branchName ==~ /^(hotfix\/.+)$/) {
            //workaround to avoid deployment on qa env from hotfix branch on the first execution
            environmentsToDeploy.add(0, "")
        }

        List jobParameters = [["parameter"     : script.string(name: 'deploy_version', defaultValue: '', description: 'Set artifact version for skip all steps and deploy only or leave empty for start full build'),
                               "branchingModel": ["gitflow"   : /^(release)\/.+$/,
                                                  "trunkbased": /^master$/],
                              ],
                              ["parameter"     : script.choice(choices: environmentsToDeploy, description: 'Where deploy?', name: 'deployDst'),
                               "branchingModel": ["gitflow"   : /^((hotfix|release)\/.+)$/,
                                                  "trunkbased": /^master$/],
                              ]]

        jobParameters.each {
            Pattern branchPattern = Pattern.compile(it.get("branchingModel").get(branchingModel))
            if (branchName ==~ branchPattern) {
                paramlist.add(it.get("parameter"))
            }
        }

        return paramlist
    }


    Map toMap() {
        Map result = [:]
        result.put("jobTriggers", jobTriggers)
        result.put("buildDaysToKeepStr", buildDaysToKeepStr)
        result.put("buildNumToKeepStr", buildNumToKeepStr)
        result.put("buildArtifactDaysToKeepStr", buildArtifactDaysToKeepStr)
        result.put("buildArtifactNumToKeepStr", buildArtifactNumToKeepStr)
        result.put("paramlist", paramlist)
        result.put("auth", auth)

        return result
    }

    @NonCPS
    Map getParams() {
        script.echo("111")
        Map jobMap = toMap()
        script.echo("222")
        def params = script.jobWithProperties(jobMap)
        script.echo("333")
        return params
    }

}

package com.nextiva.environment

import com.cloudbees.groovy.cps.NonCPS
import com.nextiva.config.Branch
import com.nextiva.config.BranchingModel
import com.nextiva.config.GitFlow
import com.nextiva.config.TrunkBased

import java.util.regex.Pattern

class Environment {
    String name
    private Pattern branchPattern = null
    Branch branchGitflow
    Branch branchTrunkbased

    String kubernetesCluster
    String kubernetesConfigSet
    String kubernetesNamespace = "default"

    String ansiblePlaybookPath
    String ansibleInventoryPath
    String ansibleInventory

    List<String> healthChecks = []

    @NonCPS
    void setBranchPattern(Object branchPattern) {
        if (branchPattern == null) {
            this.@branchPattern = null
        } else if (branchPattern instanceof String) {
            this.@branchPattern = (branchPattern == null) ? null : ~branchPattern
        } else if (branchPattern instanceof Pattern) {
            this.@branchPattern = branchPattern
        }
    }

    @NonCPS
    Pattern getBranchPattern() {
        return this.@branchPattern
    }

    @NonCPS
    Pattern getBranchPattern(BranchingModel branchingModel) {
        if (this.@branchPattern)
            return this.@branchPattern
        switch (branchingModel.class) {
            case GitFlow.class:
                return this.@branchGitflow?.branchPattern
                break
            case (TrunkBased.class):
                return this.@branchTrunkbased?.branchPattern
                break
            default:
                return this.@branchPattern
        }
    }
}


package com.nextiva.branch.model

interface BranchingModel {
    String getName()

    List getStages(String branchName)

    String getRepository(String branchName)

    List getAllowedEnvs(String branchName)
}
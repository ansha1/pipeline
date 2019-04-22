package com.nextiva.branch.model

class TrunkBase implements BranchingModel {
    @Override
    List getStages(String branchName) {
        return null
    }

    @Override
    String getRepository(String branchName) {
        return null
    }

    @Override
    List getAllowedEnvs(String branchName) {
        return null
    }

    @Override
    String getName() {
        return getClass().getSimpleName()
    }
}
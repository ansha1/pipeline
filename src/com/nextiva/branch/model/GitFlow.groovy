package com.nextiva.branch.model

class GitFlow implements BranchingModel{
    @Override
    String getRepository(String branchName) {
        switch (branchName){
            case ~/^master|hotfix\/.+|release\/.+$/:
                return "production"
        }
        return null
    }

    @Override
    List getAllowedEnvs(String branchName) {
        return null
    }

    @Override
    List getStages(String branchName){
        switch (branchName){
            case ~/(dev|develop)$/:
                break
            case ~/^release\/.+$/:
                break
            case "master":
                break
            case ~/^hotfix\/.+$/:
                break
            default:
                break


        }
        return flow
    }

    @Override
    String getName() {
        return getClass().getSimpleName()
    }
}






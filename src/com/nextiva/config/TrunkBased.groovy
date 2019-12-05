package com.nextiva.config

class TrunkBased implements BranchingModel {
    public static final Branch trunk = new Branch("trunk", BranchingModelRegexps.master)
    public static final Branch feature = new Branch("feature", BranchingModelRegexps.notMaster)

    @Override
    Branch getBranchType(String branchName) {
        if(isTrunk(branchName)) {
            return trunk
        } else {
            return feature
        }
    }

    static boolean isTrunk(String branchName) {
        return trunk.branchPattern ==~ branchName
    }
}
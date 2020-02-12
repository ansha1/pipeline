package com.nextiva.config

class GitFlow implements BranchingModel {
    public static final Branch master = new Branch("master", BranchingModelRegexps.master)
    public static final Branch develop = new Branch("develop", BranchingModelRegexps.develop)
    public static final Branch release = new Branch("release", BranchingModelRegexps.release)
    public static final Branch hotfix = new Branch("release", BranchingModelRegexps.hotfix)
    public static final Branch releaseOrHotfix = new Branch("release", BranchingModelRegexps.releaseOrHotfix)
    public static final Branch feature = new Branch("feature", BranchingModelRegexps.notMainline)

    @Override
    Branch getBranchType(String branchName) {
        if (isDevelop(branchName)) {
            return develop
        } else if (isRelease(branchName)) {
            return release
        } else if (isMaster(branchName)) {
            return master
        } else if (isHotfix(branchName)) {
            return hotfix
        } else {
            return feature
        }
    }

    static boolean isDevelop(String branchName) {
        return branchName ==~ develop.branchPattern
    }

    static boolean isMaster(String branchName) {
        return branchName ==~ master.branchPattern
    }

    static boolean isRelease(String branchName) {
        return branchName ==~ release.branchPattern
    }

    static boolean isHotfix(String branchName) {
        return branchName ==~ hotfix.branchPattern
    }
}

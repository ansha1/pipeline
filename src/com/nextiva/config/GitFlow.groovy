package com.nextiva.config

class GitFlow implements BranchingModel {
    public static final Branch master = new Branch("master", BranchingModelRegexps.master)
    public static final Branch develop = new Branch("develop", BranchingModelRegexps.develop)
    public static final Branch release = new Branch("release", BranchingModelRegexps.release)
    public static final Branch hotfix = new Branch("release", BranchingModelRegexps.hotfix)
    public static final Branch releaseOrHotfix = new Branch("release", BranchingModelRegexps.releaseOrHotfix)
}
package com.nextiva.config

import java.util.regex.Pattern

class BranchingModelRegexps {
    public static final Pattern any = ~/.+/
    public static final Pattern notMaster = ~/^(?!master$).+/
    public static final Pattern releaseOrHotfix = ~/^((hotfix|release)\/.+)/
    public static final Pattern master = ~/^master$/
    public static final Pattern mainline = ~/^((dev|develop)$|((hotfix|release)\/.+))/
    public static final Pattern mainlineWithMaster = ~/^((dev|develop|master)$|(hotfix|release)\/.+)/
    public static final Pattern notMainline = ~/^(?!(dev|develop|master)$|(hotfix|release)\/).+/
    public static final Pattern develop = ~/^(develop|dev)$/
    public static final Pattern release = ~/^release\/.+/
    public static final Pattern hotfix = ~/^hotfix\/.+/
}
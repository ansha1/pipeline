package com.nextiva.config

import hudson.AbortException

import java.util.regex.Pattern


interface BranchingModel {
    Branch getBranchType(String branchName)
}




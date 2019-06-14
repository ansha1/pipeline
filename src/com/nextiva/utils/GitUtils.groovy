package com.nextiva.utils

import com.cloudbees.groovy.cps.NonCPS
import jenkins.model.Jenkins
import static com.nextiva.utils.Utils.shWithOutput

class GitUtils {

    static clone(script, String repository, String branch, String folder = "") {
        return shWithOutput(script, "git clone $repository --branch $branch --single-branch $folder")
    }


}

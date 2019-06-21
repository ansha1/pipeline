package com.nextiva.utils

import static com.nextiva.utils.Utils.shWithOutput
import static com.nextiva.SharedJobsStaticVars.GIT_CHECKOUT_CREDENTIALS

class GitUtils {

    static clone(script, String repository, String branch, String folder = "") {
        script.sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
//            return result = shWithOutput(script, "git clone --progress --verbose $repository --branch $branch --single-branch $folder")
            script.sh "git clone --progress --verbose $repository --branch $branch --single-branch $folder"
        }
    }

}

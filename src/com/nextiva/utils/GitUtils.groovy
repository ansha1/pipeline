package com.nextiva.utils

import static com.nextiva.utils.Utils.shWithOutput
import static com.nextiva.SharedJobsStaticVars.GIT_CHECKOUT_CREDENTIALS

class GitUtils {

    static clone(script, String repository, String branch, String folder = "") {
        script.dir(folder) {
            script.git branch: branch, credentialsId: GIT_CHECKOUT_CREDENTIALS, url: repository
        }
//        script.sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
//            script.container("jnlp") {
//            return shWithOutput(script, "git clone --progress --verbose $repository --branch $branch --single-branch $folder")
////                script.sh "git clone --progress --verbose $repository --branch $branch $folder"
//            }
//        }
    }
}

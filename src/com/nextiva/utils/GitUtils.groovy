package com.nextiva.utils

import static com.nextiva.SharedJobsStaticVars.GIT_CHECKOUT_CREDENTIALS

class GitUtils {

    static void clone(script, String repository, String branch, String folder = "") {
        script.dir(folder) {
            script.git branch: branch, credentialsId: GIT_CHECKOUT_CREDENTIALS, url: repository
        }
    }
}

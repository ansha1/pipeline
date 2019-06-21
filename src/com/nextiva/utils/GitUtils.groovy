package com.nextiva.utils

import static com.nextiva.utils.Utils.shWithOutput

class GitUtils {

    static clone(script, String repository, String branch, String folder = "") {
        return shWithOutput(script, "git clone --progress --verbose $repository --branch $branch --single-branch $folder")
    }


}

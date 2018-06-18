import static com.nextiva.SharedJobsStaticVars.*


def createVirtualEnv(String pythonName='python', String venvDir=VENV_DIR) {
    log.info("Create virtualenv (${venvDir})")
    sh(script: "virtualenv --python=${pythonName} ${venvDir}")
}

def getVirtualEnv(String venvDir=VENV_DIR) {
    if ( ! fileExists(venvDir) ) {
        currentBuild.rawBuild.result = Result.ABORTED
        throw new hudson.AbortException("ERROR: There is no virtualenv dir - ${venvDir}.")
    }
    
    // get the absolute path of a env dir
    dir(venvDir){
        absoluteVenvDir = pwd()
    }

    if(env.DEPLOY_ENVIRONMENT.equals(null) || env.DEPLOY_ENVIRONMENT == '') {
        pipRepo = PIP_EXTRA_INDEX_DEFAULT_REPO
    }
    else {
        pipRepo = env.DEPLOY_ENVIRONMENT
    }

    return [
        "VIRTUAL_ENV=${absoluteVenvDir}",
        "PYTHONDONTWRITEBYTECODE=1",
        "PATH=${absoluteVenvDir}/bin:${env.PATH}",
        "PIP_TRUSTED_HOST=${PIP_TRUSTED_HOST}",
        "PIP_INDEX_URL=${PIP_EXTRA_INDEX_URL}${pipRepo}${PIP_EXTRA_INDEX_URL_SUFFIX}",
    ]
}

def venvSh(String cmd, Boolean returnStdout=false, String venvDir=VENV_DIR) {
    log.info("Activate virtualenv and run command (${venvDir})")
    withEnv(getVirtualEnv(venvDir)) {
        output = sh(returnStdout: returnStdout, script: cmd)
    }
    return output
}

def pipInstall(String filename, String venvDir=VENV_DIR) {
    withEnv(getVirtualEnv(venvDir)) {
        sh(script: "pip install -r ${filename}")
    }
}

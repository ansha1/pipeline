import static com.nextiva.SharedJobsStaticVars.*


def createVirtualEnv(String pythonName='python', String venvDir=VENV_DIR) {
    println 'Create virtualenv.'
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

    pipRepo = env.DEPLOY_ENVIRONMENT.equals(null) ? PIP_EXTRA_INDEX_DEFAULT_REPO : env.DEPLOY_ENVIRONMENT
    if(env.DEPLOY_ENVIRONMENT == null){
        echo "env.DEPLOY_ENVIRONMENT == null"
    }
    if(env.DEPLOY_ENVIRONMENT){
        echo "env.DEPLOY_ENVIRONMENT == true"
    }
    echo instanceof env.DEPLOY_ENVIRONMENT
    echo "pipRepo - ${pipRepo}"

    return [
        "VIRTUAL_ENV=${absoluteVenvDir}",
        "PYTHONDONTWRITEBYTECODE=1",
        "PATH=${absoluteVenvDir}/bin:${env.PATH}",
        "PIP_TRUSTED_HOST=${PIP_TRUSTED_HOST}",
        "PIP_INDEX_URL=${PIP_EXTRA_INDEX_URL}${pipRepo}${PIP_EXTRA_INDEX_URL_SUFFIX}",
    ]
}

def venvSh(String cmd, Boolean returnStdout=false, String venvDir=VENV_DIR) {
    println "Activate virtualenv and run command (${venvDir})."
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

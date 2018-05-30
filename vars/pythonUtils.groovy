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
    return [
        "VIRTUAL_ENV=${venvDir}",
        "PYTHONDONTWRITEBYTECODE=1",
        "PATH=${venvDir}/bin:${env.PATH}"
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

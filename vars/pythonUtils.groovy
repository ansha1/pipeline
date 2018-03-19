import static com.nextiva.SharedJobsStaticVars.*


def createVirtualEnv(String pythonName='python', String venvDir=VENV_DIR) {
    println 'Create virtualenv.'
    sh(script: "virtualenv --python=${pythonName} ${venvDir}")
}

def venvSh(String cmd, returnStdout=false, String venvDir=VENV_DIR) {
    println 'Activate virtualenv.'
    withEnv(getVirtualEnv()) {
        output = sh(returnStdout: returnStdout, script: cmd)
    }
    return output
}

def getVirtualEnv(String venvDir=VENV_DIR) {
    return [
        "VIRTUAL_ENV=${WORKSPACE}/${venvDir}/",
        "PYTHONDONTWRITEBYTECODE=1",
        "PATH=${WORKSPACE}/${venvDir}/bin:${env.PATH}"
    ]
}

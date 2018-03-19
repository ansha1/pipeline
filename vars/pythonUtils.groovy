import static com.nextiva.SharedJobsStaticVars.*


def createVirtualEnv(String pythonName='python') {
    println 'Create virtualenv.'
    sh(script: "virtualenv --python=${pythonName} ${VENV_DIR}")
}

def venvSh(String cmd, String venvDir=VENV_DIR, returnStdout=false) {
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

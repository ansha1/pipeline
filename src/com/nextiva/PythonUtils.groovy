package com.nextiva;
import static com.nextiva.SharedJobsStaticVars.*


def createVirtualEnv(String pythonVersion='python3.6') {
    println 'Create virtualenv.'
    sh(script: "virtualenv --python=${pythonVersion} ${VENV_DIR}")
}

def venvSh(String cmd, String venvDir=VENV_DIR) {
    println 'Activate virtualenv.'
    withEnv(getVirtualEnv()) {
        sh(script: cmd)
    }
}

def getVirtualEnv(String venvDir=VENV_DIR) {
    return [
        "VIRTUAL_ENV=${WORKSPACE}/${venvDir}/",
        "PYTHONDONTWRITEBYTECODE=1",
        "PATH=${WORKSPACE}/${venvDir}/bin:${env.PATH}"
    ]
}

return this

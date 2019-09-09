import static com.nextiva.SharedJobsStaticVars.*


def createVirtualEnv(String pythonName = 'python', String venvDir = VENV_DIR) {
    log.info("Create virtualenv (${venvDir})")
    sh "${pythonName} -m venv ${venvDir}"
//    sh "virtualenv --python=${pythonName} ${venvDir}"
}

def getVirtualEnv(String venvDir = VENV_DIR) {
    if (!fileExists(venvDir)) {
        currentBuild.rawBuild.result = Result.ABORTED
        throw new hudson.AbortException("There is no virtualenv dir - ${venvDir}.")
    }

    // get the absolute path of a env dir
    dir(venvDir) {
        absoluteVenvDir = pwd()
    }

    pipRepo = getPipRepo()

    return [
            "VIRTUAL_ENV=${absoluteVenvDir}",
            "PYTHONDONTWRITEBYTECODE=1",
            "PATH=${absoluteVenvDir}/bin:${env.PATH}",
            "PIP_INDEX_URL=${PIP_EXTRA_INDEX_URL}${pipRepo}${PIP_EXTRA_INDEX_URL_SUFFIX}",
    ]
}

def venvSh(String cmd, Boolean returnStdout = false, String venvDir = VENV_DIR) {
    log.info("Activate virtualenv and run command (${venvDir})")

    // Workaround to change PATH variable inside docker container
    // https://issues.jenkins-ci.org/browse/JENKINS-49076
    def envVars = getVirtualEnv(venvDir)
    def pathVars = envVars.findAll { it ==~ /^PATH\=.*/ }.join("\n")
    cmd = "${pathVars}\n${cmd}"

    withEnv(envVars) {
        output = sh(name: 'Run sh script', returnStdout: returnStdout, script: cmd)
    }
    return output
}

def pipInstall(String filename, String venvDir = VENV_DIR) {
    withEnv(getVirtualEnv(venvDir)) {
        sh "pip install -r ${filename}"
    }
}

def getPipRepo() {
    if (env.DEPLOY_ENVIRONMENT) {
        return env.DEPLOY_ENVIRONMENT
    } else if (env.BRANCH_NAME ==~ /PR-.*/) {
        def dstBranch = bitbucket.getDestinationBranchFromPr(env.CHANGE_URL)
        def pipRepo = PIP_EXTRA_INDEX_DEFAULT_REPO
        switch (dstBranch) {
            case 'dev':
                pipRepo = 'dev'
                break
            case 'develop':
                pipRepo = 'dev'
                break
            case ~/^release\/.+$/:
                pipRepo = 'production'
                break
            case ~/^hotfix\/.+$/:
                pipRepo = 'production'
                break
            case 'master':
                pipRepo = 'production'
                break
        }
        return pipRepo
    } else {
        return PIP_EXTRA_INDEX_DEFAULT_REPO
    }
}

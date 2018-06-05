import static com.nextiva.SharedJobsStaticVars.*


def call(String repoDir, String inventoryPath, String playbookPath, Map extraVars) {

    node(NODE_NAME) {
        def playbookContext = getPlaybookContext(inventoryPath, playbookPath, extraVars)
        execute(repoDir, playbookContext, playbookPath)
    }
}

def releaseManagement(String inventoryPath, String playbookPath, Map extraVars) {
    
    node(ANSIBLE_NODE_LABEL) {
        def repoDir = prepareRepoDir(RELEASE_MANAGEMENT_REPO_URL, RELEASE_MANAGEMENT_REPO_BRANCH)
        def playbookContext = getPlaybookContext(inventoryPath, playbookPath, extraVars)
        execute(repoDir, playbookContext, playbookPath)
    }
}

def getPlaybookContext(String inventoryPath, String playbookPath, Map extraVars) {

    def generateExtraVars = ''

    extraVars.each { key, value ->
        generateExtraVars +=  ' --extra-vars "' + key + '=' + value + '"'
    }

    def playbookContext = '-i ' + inventoryPath + ' ' + playbookPath +
                          ' --vault-password-file ' + ANSIBLE_PASSWORD_PATH + generateExtraVars

    println "playbookContext: " + playbookContext

    return playbookContext
}

def execute(String repoDir, String playbookContext, String playbookPath) {
    script {
      	checkRCState()
        stage('Run ansible playbook ' + playbookPath) {
            sh """
                cd ${repoDir} && ansible-playbook ${playbookContext}
            """
        }
    }
}

def checkRCState() {
    // check if RC in locked state
    if (env.BRANCH_NAME ==~ ~/^release\/.+$/ && isRCLocked()) {
        currentBuild.rawBuild.result = Result.ABORTED
        throw new hudson.AbortException(RC_JOB_LOCK_MESSAGE)
    }
}

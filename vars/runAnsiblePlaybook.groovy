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
        stage('Run ansible playbook ' + playbookPath) {
            checkRCState()
            sh """
                cd ${repoDir} && ansible-playbook ${playbookContext}
            """
        }
    }
}

def checkRCState() {
    
    // check if RC in locked state
    // should be used with options { skipStagesAfterUnstable() } in Jenkins file
    if (env.BRANCH_NAME ==~ ~/^release\/.+$/ && isRCLocked()) {
        echo 'All RC deploy jobs are locked !!!\n' +
           'Please contact QA Core Team.'
        currentBuild.result = 'UNSTABLE'
    }
}
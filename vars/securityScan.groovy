def call(def jobConfig) {
    String language = jobConfig.projectFlow.get('language')
    String languageVersion = jobConfig.projectFlow.get('languageVersion', 'python3.6')
    String pathToSrc = jobConfig.projectFlow.get('pathToSrc', '.')

    switch (language) {
        case 'java':
            runVeracodeScanJava(jobConfig)
            break
        case 'js':
            runSourceClearJs(pathToSrc)
            break
        case 'python':
            runSourceClearScannerPython(pathToSrc, languageVersion)
            break
        default:
            log.warn("Security scan is unavailable for your language ${language}")
    }
}


def runVeracodeScanJava(def jobConfig) {
    try {
        utils = jobConfig.getUtils()
        utils.buildForVeracode(jobConfig.APP_NAME, jobConfig.BUILD_VERSION, jobConfig.DEPLOY_ENVIRONMENT, jobConfig.projectFlow)
        withCredentials([usernamePassword(credentialsId: 'veracode', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
            veracode applicationName: jobConfig.veracodeApplicationScope,
                    criticality: 'VeryHigh',
                    createSandbox: true,
                    sandboxName: jobConfig.APP_NAME,
                    debug: true,
                    scanName: "${jobConfig.APP_NAME}-${jobConfig.BUILD_VERSIO}", timeout: 20,
                    fileNamePattern: "",
                    scanIncludesPattern: "",
                    uploadIncludesPattern: "**/target/*.war, **/target/*.jar",
                    vuser: USERNAME, vpassword: PASSWORD
        }
    } catch (e) {
        log.warn("Veracode scan fail ${e}")
    }

}

def runSourceClearJs(String pathToSrc) {
    log.info('============================')
    log.info('Start source clear scan for Js applications')
    log.info('============================')
    dir(pathToSrc) {
        try {
            def sourceClearCi = libraryResource 'sourceclear/sc.sh'
            def creds = tokenFromRepo(env.GIT_URL)
            withCredentials([string(credentialsId: creds, variable: 'SRCCLR_API_TOKEN')]) {
                withEnv(["SRCCLR_CI_JSON=1", "DEBUG=1", "NOCACHE=1"]) {
                    sh "${sourceClearCi}"
                }
            }
        } catch (e) {
            log.warn("Sourceclear scan fail ${e}")
        }
    }
}


/ *
 source clear scans from python are different, if you do not set up the virtual environment the scan will fail, 
 not sure how to make that work in a vars class.
*/

def runSourceClearScannerPython(String pathToSrc, String languageVersion) {
    log.info('============================')
    log.info('Start source clear scan for python applications')
    log.info('============================')
    dir(pathToSrc) {
        try {
            def sourceClearCi = libraryResource 'sourceclear/sc.sh'
            def creds = tokenFromRepo(env.GIT_URL)
            pythonUtils.createVirtualEnv(languageVersion)
            withCredentials([string(credentialsId: creds, variable: 'SRCCLR_API_TOKEN')]) {
                withEnv(["SRCCLR_CI_JSON=1", "DEBUG=1", "NOCACHE=1"]) {
                    pythonUtils.venvSh """${sourceClearCi}"""
                }
            }
        } catch (e) {
            log.warn("Sourceclear scan fail ${e}")
        }

    }
}


String tokenFromRepo(String repo) {
    switch (repo) {
        case ~/(?i).*crm.*/:
            credName = 'CRM_SRCCLR'
            break
        case ~/(?i).*analytics.*/:
            credName = 'ANALYTICS_SRCCLR'
            break
        case ~/(?i).*dash.*/:
            credName = 'DASHBOARD_SRCCLR'
            break
        case ~/(?i).*rengine.*/:
            credName = 'RULES_SRCCLR'
            break
        case ~/(?i).*surveys.*/:
            credName = 'SURVEYS_SRCCLR'
            break
        case ~/(?i).*migration.*/:
            credName = 'DM_SRCCLR'
            break
        case ~/(?i).*realtalk.*/:
            credName = 'RT_SRCCLR'
            break
        case ~/(?i).*platform.*/:
            credName = 'PLATFORM_SRCCLR'
            break
        default:
            credName = null
    }
    return credName
}
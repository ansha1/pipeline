def call(String appName, String language, String languageVersion, String repoUrl, String pathToSrc) {

    switch (language) {
        case 'java':
            runVeracodeScanJava(appName, pathToSrc)
            break
        case 'js':
            runSourceClearScanner(pathToSrc, repoUrl) {
                def sourceClearCi = libraryResource 'sourceclear/ci.sh'
                sh "${sourceClearCi}"
            }
            break
        case 'python':
            runSourceClearScanner(pathToSrc, repoUrl) {
                def sourceClearCi = libraryResource 'sourceclear/ci.sh'
                pythonUtils.createVirtualEnv(languageVersion)
                pythonUtils.venvSh "${sourceClearCi}"
            }

            break
        default:
            log.warn("Security scan is unavailable for your language ${language}")
    }
}


def runVeracodeScanJava(String appName, String pathToSrc) {
    try {
        def utils = getUtils("java", pathToSrc)

        String buildVersion = utils.getVersion()

        utils.buildForVeracode(appName, buildVersion, "production", ["veracodeBuildCommands": "mvn clean install -U --batch-mode -DskipTests"])
        withCredentials([usernamePassword(credentialsId: 'veracode', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
            veracode applicationName: DEFAULT_VERACODE_APPLICATION_SCOPE,
                    criticality: 'VeryHigh',
                    createSandbox: true,
                    sandboxName: appName,
                    debug: true,
                    scanName: "${appName}-${buildVersion}", timeout: 20,
                    fileNamePattern: "",
                    scanIncludesPattern: "",
                    uploadIncludesPattern: "**/target/*.war, **/target/*.jar",
                    vuser: USERNAME, vpassword: PASSWORD
        }
    } catch (e) {
        log.warn("Veracode scan fail ${e}")
    }
}

def runSourceClearScanner(String pathToSrc, String repoUrl, body) {
    log.info('============================')
    log.info('Start source clear scan')
    log.info('============================')
    dir(pathToSrc) {
        try {
            def creds = tokenFromRepo(repoUrl)
            withCredentials([string(credentialsId: creds, variable: 'SRCCLR_API_TOKEN')]) {
                withEnv(["SRCCLR_CI_JSON=1", "DEBUG=1", "NOCACHE=1"]) {
                    body()
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
            log.info("SourceClearScanner is not available for your repo")
            credName = null
    }
    return credName
}
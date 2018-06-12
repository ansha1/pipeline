def run(String projectVersion='0.1.0') {
    try {
        scannerHome = tool SONAR_QUBE_SCANNER

        dir(pathToSrc) {
            withSonarQubeEnv(SONAR_QUBE_ENV) {
                sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectVersion=${projectVersion}"
            }
        }

        timeout(time: 10, unit: 'MINUTES') {
            def qg = waitForQualityGate()
            if (qg.status != 'OK') {
                print('Sonar Quality Gate failed')
                // currentBuild.rawBuild.result = Result.UNSTABLE
            }
        }
    } catch (e) {
        print e
        currentBuild.rawBuild.result = Result.UNSTABLE
    }
}

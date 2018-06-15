import static com.nextiva.SharedJobsStaticVars.*

def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    appName = pipelineParams.appName
    buildVersion = pipelineParams.buildVersion
    repoUrl = pipelineParams.repoUrl
    repoBranch = pipelineParams.repoBranch

    node {
        properties properties: [
                disableConcurrentBuilds()
        ]
        cleanWs()

        stage("checkout") {
            git branch: repoBranch, credentialsId: GIT_CHECKOUT_CREDENTIALS, url: repoUrl
        }
        stage("create archive") {
            sh "zip -rv9 ${appName}-${buildVersion}.zip . -i '*.py' '*.html' '*.htm'"
        }
        stage("start veracode scan") {
            withCredentials([usernamePassword(credentialsId: 'veracode', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                veracode applicationName: 'NextOS Platform (CRM)',
                        criticality: 'VeryHigh',
                        fileNamePattern: "${appName}-${buildVersion}.zip",
                        scanIncludesPattern: "${appName}-${buildVersion}.zip",
                        scanName: "${appName}-${buildVersion}", timeout: 240,
                        uploadIncludesPattern: "${appName}-${buildVersion}.zip",
                        vuser: USERNAME, vpassword: PASSWORD
            }
        }
    }
}
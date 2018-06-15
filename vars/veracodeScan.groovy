import static com.nextiva.SharedJobsStaticVars.*

def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    APP_NAME = pipelineParams.appName
    BUILD_VERSION = pipelineParams.appName
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
            sh "zip -rv9 ${APP_NAME}-${BUILD_VERSION}.zip . -i '*.py' '*.html' '*.htm'"
        }
        stage("start veracode scan") {
            withCredentials([usernamePassword(credentialsId: 'veracode', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                veracode applicationName: 'NextOS Platform (CRM)',
                        criticality: 'VeryHigh',
                        fileNamePattern: "${APP_NAME}-${BUILD_VERSION}.zip",
                        scanIncludesPattern: "${APP_NAME}-${BUILD_VERSION}.zip",
                        scanName: "${APP_NAME}-${BUILD_VERSION}", timeout: 240,
                        uploadIncludesPattern: "${APP_NAME}-${BUILD_VERSION}.zip",
                        vuser: USERNAME, vpassword: PASSWORD
            }
        }
    }
}
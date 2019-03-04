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
    projectLanguage = pipelineParams.projectLanguage
    veracodeApplicationScope = pipelineParams.veracodeApplicationScope ?: 'NextOS Platform (CRM)'
    upstreamNodeName = pipelineParams.upstreamNodeName
    upstreamWorkspace = pipelineParams.upstreamWorkspace
    javaArtifactsProperties = pipelineParams.javaArtifactsProperties


    node(upstreamNodeName) {
        properties properties: [
                disableConcurrentBuilds()
        ]

        cleanWs()

        timestamps {
            timeout(time: 240, unit: 'MINUTES') {
                switch (projectLanguage) {
                    case 'python':
                    case 'js':
                        fileNamePattern = "${appName}-${buildVersion}.zip"
                        scanIncludesPattern = "${appName}-${buildVersion}.zip"
                        uploadIncludesPattern = "${appName}-${buildVersion}.zip"

                        stage("checkout") {
                            scanIncludesPattern
                            git branch: repoBranch, credentialsId: GIT_CHECKOUT_CREDENTIALS, url: repoUrl
                        }
                        stage("create archive") {
                            sh "zip -rv9 ${appName}-${buildVersion}.zip . -i '*.py' '*.js' '*.html' '*.htm'"
                        }
                        break
                    case 'java':
                        stage("getting java artifacts from upstreamJob") {
                            fileNamePattern = "" //this creates a pattern to rename with, don't use it.
                            scanIncludesPattern = "" //not necessary either
                            //we copy all the files we want uploaded into the upstream workspace folder, so *.war, *.jar is all that is necessary.
                            uploadIncludesPattern = "*.war, *.jar"
                            try {
                                log.debug("javaArtifactsProperties: ${javaArtifactsProperties}")
                                List artifactsListProperties = []

                                javaArtifactsProperties.split('\n').each {
                                    def propertiesList = it.split()
                                    artifactsListProperties << ['groupId': propertiesList[0], 'artifactVersion': propertiesList[2], 'artifactId': propertiesList[1], 'packaging': propertiesList[3], 'finalName': propertiesList[4]]
                                }

                                artifactsListProperties.each { artifact ->
                                    if (artifact.packaging == 'pom' ) return

                                    /**
                                     *  Veracode does not support spring-boot applications, after buildForVeracode is called in JavaUtils the resulting jar/war files need to be moved
                                     *  into the root workspace so veracode can find them. wildcard patterns don't appear to work properly to traverse directories with the veracode plugin
                                     *  so we copy them over manually instead
                                     *
                                     */

                                    // define the file we are going to be copying to

                                    def outputFile = "$WORKSPACE/$artifact.artifactId.$artifact.packaging"
                                    log.info("outputFile: $outputFile")

                                    //copy the target artifact to the root directory ie: cp /opt/jenkins/workspace/sales-quotation-v2_jenkinstest/salesquotationv2-common/target/SalesQuotePortalV2.jar .
                                    sh "cp $upstreamWorkspace/$artifact.artifactId/target/$artifact.finalName.$artifact.packaging ."

                                }
                            } catch (e) {
                                log.warn("can`t download artifacts $e")
                            }

                        }
                        break
                }

                stage("Start Veracode Scan") {
                    echo 'scan'
                    withCredentials([usernamePassword(credentialsId: 'veracode', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                        veracode applicationName: veracodeApplicationScope,
                                criticality: 'VeryHigh',
                                createSandbox: true,
                                sandboxName: appName,
                                debug: true,
                                scanName: "${appName}-${buildVersion}", timeout: 240,
                                fileNamePattern: fileNamePattern,
                                scanIncludesPattern: scanIncludesPattern,
                                uploadIncludesPattern: uploadIncludesPattern,
                                vuser: USERNAME, vpassword: PASSWORD
                    }
                }
            }
        }
    }
}

def dynamicRescan() {
    node {
        stage("Start Veracode Dynamic Rescan") {
            withCredentials([usernamePassword(credentialsId: 'veracode', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                veracodeDynamicRescan applicationName: 'NextOS Platform (CRM)',
                        debug: true,
                        vuser: USERNAME, vpassword: PASSWORD
            }
        }
    }
}

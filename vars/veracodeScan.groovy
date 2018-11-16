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
                            try {
                                echo "javaArtifactsProperties: ${javaArtifactsProperties}"
                                List artifactsListProperties = []

                                javaArtifactsProperties.split('\n').each {
                                    def propertiesList = it.split()
                                    artifactsListProperties << ['groupId': propertiesList[0], 'artifactVersion': propertiesList[2], 'artifactId': propertiesList[1], 'packaging': propertiesList[3]]
                                }

                                echo "artifactsListProperties: ${artifactsListProperties}"
                                artifactsListProperties.each { artifact ->
                                    if (artifact.packaging == 'pom' ) return
                                    if (nexus.isJavaArtifactExists(artifact.groupId, artifact.artifactId, artifact.artifactVersion, artifact.packaging)) {
                                        log.info("this artifact ${artifact.artifactId} ${artifact.artifactVersion} needs to be downloaded")
                                        log.info("curl --insecure http://repository.nextiva.xyz:8081/nexus/service/local/repositories/releases/content/com/nextiva/${artifact.artifactId}/${artifact.artifactVersion}/${artifact.artifactId}-${artifact.artifactVersion}.${artifact.packaging} > $WORKSPACE/${artifact.artifactId}.${artifact.packaging}")
                                        sh "curl --insecure http://repository.nextiva.xyz:8081/nexus/service/local/repositories/releases/content/com/nextiva/${artifact.artifactId}/${artifact.artifactVersion}/${artifact.artifactId}-${artifact.artifactVersion}.${artifact.packaging} > $WORKSPACE/${artifact.artifactId}.${artifact.packaging}"
                                    }
                                }


//                                http://repository.nextiva.xyz:8081/nexus/service/local/repositories/releases/content/com/nextiva/calendar-service/1.13.0/calendar-service-1.13.0.war
//                                  http://repository.nextiva.xyz:8081/nexus/service/local/artifact/maven/resolve
//                                sh "cp $upstreamWorkspace/**/target/*.jar $WORKSPACE"
//                                sh "cp $upstreamWorkspace/**/target/*.war $WORKSPACE"
                            } catch (e) {
                                log.warn("can`t cp files $e")
                            }
                            fileNamePattern = "*.*"
                            scanIncludesPattern = "*.*"
                            uploadIncludesPattern = "*.*"
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

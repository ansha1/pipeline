#!groovy
import com.nextiva.*

import static com.nextiva.SharedJobsStaticVars.*

def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    repositoryUrl = pipelineParams.repositoryUrl
    projectLanguage = pipelineParams.projectLanguage
    hotfixVersion = pipelineParams.hotfixVersion
    versionPath = pipelineParams.versionPath.equals(null) ? '.' : pipelineParams.versionPath

//noinspection GroovyAssignabilityCheck
    pipeline {

        agent any

        options {
            timestamps()
            skipStagesAfterUnstable()
        }
        tools {
            jdk 'Java 8 Install automatically'
            maven 'Maven 3.3.3 Install automatically'
        }
        stages {
            stage('Checkout repo') {
                steps {
                    cleanWs()

                    git branch: 'master', credentialsId: GIT_CHECKOUT_CREDENTIALS, url: repositoryUrl

                }
            }
            stage('Prepare for starting release') {
                steps {
                    script {

                        switch (projectLanguage) {
                            case 'java':
                                utils = new JavaUtils()
                                break
                            case 'python':
                                utils = new PythonUtils()
                                break
                            case 'js':
                                utils = new JsUtils()
                                break
                            default:
                                error("""Incorrect programming language
                                        please set one of the
                                        supported languages:
                                        java
                                        python
                                        js""")
                                break
                        }
                    }
                }
            }
            stage('Collecting hotfix version') {
                steps {
                    script {
                        echo "\nUserDefinedHotfixVersion: ${hotfixVersion}\n"
                        hotfixVersion = hotfixVersion.equals('') ? getNextVersion(utils, versionPath) : hotfixVersion

                        if (hotfixVersion ==~ /^(\d+.\d+.\d+)$/) {
                            echo("\n\nSelected hotfix version: ${hotfixVersion}")
                        } else {
                            error("""\n\nWrong hotfix version : ${hotfixVersion}
                                    please use git-flow naming convention\n\n""")
                        }
                    }
                }
            }
            stage('Create hotfix branch') {
                steps {
                    script {
                        utils.setVersion(hotfixVersion, versionPath)
                        sh """
                          git checkout -b hotfix/${hotfixVersion}
                          git commit -a -m "Release engineering - bumped to ${hotfixVersion} patch version "
                        """
                    }
                }
            }
            stage('Push to bitbucket repo') {
                steps {
                    script {
                        sh """
                          git push --all
                        """
                    }
                }
            }
        }
    }
}

String getNextVersion(utils, versionPath) {
    def version = utils.getVersion(versionPath)
    if (!(version ==~ /^(\d+.\d+.\d+)$/)) {
        error("Wrong hotfix version: '${version}'")
    }
    def tokens = version.tokenize('.')
    def major = tokens.get(0)
    def minor = tokens.get(1)
    def patch = tokens.get(2)
    return major + "." + minor + "." + (patch.toInteger() + 1)
}
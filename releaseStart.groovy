#!groovy
@Library('pipelines') _
import static com.nextiva.SharedJobsStaticVars.*

//noinspection GroovyAssignabilityCheck
pipeline {
    agent any
    tools {
        jdk 'Java 8 Install automatically'
        maven 'Maven 3.3.3 Install automatically'
    }
    parameters {
        choice(choices: 'java\njavascript\npython', description: '', name: 'projectType')
        string(defaultValue: 'ssh://git@git.nextiva.xyz:7999/rel/testwebhook.git', description: 'repository url where you want to create release', name: 'repositoryUrl', trim: false)
        string(defaultValue: 'autoincrement', description: 'enter release version, or left empty to use version autoincrement', name: 'releaseVersion', trim: false)
    }
    stages {
        stage('Checkout repo') {
            steps {
                cleanWs()

                git branch: 'dev', credentialsId: GIT_CHECKOUT_CREDENTIALS, url: env.repositoryUrl
            }
        }

        stage('Collecting current version') {
            steps {
                script {
                    prepareVersion(env.projectType)
                }
            }
        }
        stage('Create release branch') {
            steps {
                script {
                    bumpReleaseVersion(env.projectType)
                    if (!env.projectType.equals('javascript')) {
                        sh """
                              git commit -a -m "RELEASE ENGINEERING - bumped to ${releaseVersion} release candidate version "
                           """
                    }
                    sh """
                          git branch release/${releaseVersion}
                       """
                }
            }
        }
        stage('Next development version bump') {
            steps {
                script {
                    bumpNextDevelopmentVersion(env.projectType)
                    sh """
                          git commit -a -m "RELEASE ENGINEERING - bumped to ${developmentVersion} next development version"
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

def prepareVersion(String projectType) {

    switch (projectType) {
        case 'java':
            if (releaseVersion.equals('autoincrement')) {
                rootPom = readMavenPom file: ''
                projectVersion = rootPom.version

                if (projectVersion.endsWith("-SNAPSHOT")) {
                    releaseVersion = projectVersion - "-SNAPSHOT"
                } else {
                    error("ERROR: Branch contains non-snapshot version: " + projectVersion)
                }
            } else {
                if (!releaseVersion =~ (/^\d+\.\d+\.\d+$/)) {
                    error("ERROR: Invalid release version: " + releaseVersion)
                }
            }

            def tokens = releaseVersion.tokenize('.')
            def major = tokens.get(0)
            def minor = tokens.get(1)
            def patch = tokens.get(2)
            if (patch.isNumber()) {
                developmentVersion = major + "." + (minor.toInteger() + 1) + "." + "0" + "-SNAPSHOT"
            } else {
                error("ERROR: Invalid version: " + projectVersion)
            }
            break
        case 'javascript':
            releaseVersion = sh(script: 'node -p "require(\'./package.json\').version"', returnStdout: true)
            break
    }
}

def bumpReleaseVersion(String projectType) {
    switch (projectType) {
        case 'java':
            sh "mvn versions:set -DnewVersion=${releaseVersion} -DgenerateBackupPoms=false"
            break
        case 'javascript':
            break
    }
}

def bumpNextDevelopmentVersion(String projectType) {
    switch (projectType) {
        case 'java':
            sh "mvn versions:set -DnewVersion=${developmentVersion} -DgenerateBackupPoms=false"
            break
        case 'javascript':
            sh "npm --no-git-tag-version version patch"
            developmentVersion = sh(script: 'node -p "require(\'./package.json\').version"', returnStdout: true)
            break
    }
}

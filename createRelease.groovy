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

                checkout([$class: 'GitSCM', branches: [[name: '*/dev']], userRemoteConfigs: [[credentialsId: 'jenkinsbitbucket', url: env.repositoryUrl]]])
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
                    sh """
                          git checkout -b release/${releaseVersion}
                          mvn versions:set -DnewVersion=${releaseVersion} -DgenerateBackupPoms=false
                          git commit -a -m "RELEASE ENGINEERING - Created release/${releaseVersion} branch"
                       """
                }
            }
        }
        stage('Next development version bump') {
            steps {
                script {
                    sh """
                          git checkout dev
                          mvn versions:set -DnewVersion=${developmentVersion} -DgenerateBackupPoms=false
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
                    print "ERROR: Branch contains non-snapshot version: " + projectVersion
                    currentBuild.currentResult = 'FAILED'
                }
            } else {
                if (!releaseVersion =~ (/^\d+\.\d+\.\d+$/)) {
                    print "ERROR: Invalid release version: " + releaseVersion
                    currentBuild.currentResult = 'FAILED'
                }
            }

            def tokens = releaseVersion.tokenize('.')
            def lastToken = tokens.last()
            if (lastToken.isNumber()) {
                def nextBuild = (lastToken.toInteger() + 1).toString()
                def prefix = releaseVersion - lastToken;
                developmentVersion = prefix + nextBuild + "-SNAPSHOT"
            } else {
                print "ERROR: Invalid version: " + projectVersion
                currentBuild.currentResult = 'FAILED'
            }
            break
    }
    println("Release version: " + releaseVersion);
    println("Next development version: " + developmentVersion);
}

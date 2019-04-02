import static com.nextiva.SharedJobsStaticVars.*


def deploy(String serviceName, String buildVersion, String clusterDomain, List kubernetesDeploymentsList, String nameSpace = 'default') {

    def envName = "${clusterDomain.tokenize('.').get(0)}"
    def configSet = "aws-${envName}"
    log.info("Choosen configSet is ${configSet} for ${clusterDomain}")

    kubectlInstall()
    vaultInstall()

    withEnv(["BUILD_VERSION=${buildVersion.replace('+', '-')}",
             "KUBELOGIN_CONFIG=${env.WORKSPACE}/.kubelogin",
             "KUBECONFIG=${env.WORKSPACE}/kubeconfig",
             "PATH=${env.PATH}:${WORKSPACE}"]) {

        try {
            login(clusterDomain)

            def repoDir = prepareRepoDir(KUBERNETES_REPO_URL, KUBERNETES_REPO_BRANCH)

            dir(repoDir) {
                log.info('Checking of application manifests ...')
                kubeup(serviceName, configSet, nameSpace, true)

                log.info('Deploying application into Kubernetes ...')
                kubeup(serviceName, configSet, nameSpace, false)
            }
            log.info("Deploy to the Kubernetes cluster has been completed.")

        } catch (e) {
            log.warning("Deploy to the Kubernetes failed!")
            log.warning(e)
            error("Deploy to the Kubernetes failed! $e")
        }

        sleep 15 // add sleep to avoid failures when deployment doesn't exist yet PIPELINE-93

        try {
            kubernetesDeploymentsList.each {
                sh """
                   unset KUBERNETES_SERVICE_HOST
                   kubectl rollout status deployment/${it} --namespace ${nameSpace}
                """
            }
        } catch (e) {
            log.warning("kubectl rollout status is failed!")
            log.warning("Ensure that your APP_NAME variable in the Jenkinsfile and metadata.name in app-operator manifest are the same")
            log.warning(e)
            currentBuild.rawBuild.result = Result.UNSTABLE
        }
    }
}

def login(String clusterDomain) {

    String k8sEnv = ".venv_${common.getRandomInt()}"

    withCredentials([usernamePassword(credentialsId: 'jenkinsbitbucket', usernameVariable: 'KUBELOGIN_USERNAME', passwordVariable: 'KUBELOGIN_PASSWORD'),
                     usernamePassword(credentialsId: "vault-ro-access", usernameVariable: 'VAULT_RO_USER', passwordVariable: 'VAULT_RO_PASSWORD')]) {
        def response = httpRequest quiet: !log.isDebug(), consoleLogResponseBody: log.isDebug(),
                url: "https://login.${clusterDomain}/info"
        def responseJson = readJSON text: response.content

        if (responseJson.data.build_version) {
            kubelogin_version = responseJson.data.build_version.replace('-', '+')
        }
        else {
            kubelogin_version = KUBERNETES_KUBELOGIN_DEFAULT_VERSION
        }

        log.info("Going to install kubelogin (${kubelogin_version})")
        pythonUtils.createVirtualEnv("python3", k8sEnv)
        pythonUtils.venvSh("pip3 install http://repository.nextiva.xyz/repository/pypi-dev/packages/nextiva-kubelogin/${kubelogin_version}/nextiva-kubelogin-${kubelogin_version}.tar.gz", false, k8sEnv)

        pythonUtils.venvSh("""
            unset KUBERNETES_SERVICE_HOST
            kubelogin -s login.${clusterDomain} 2>&1
            kubectl get nodes
            """, false, k8sEnv)

        // vault login
        withEnv(["VAULT_ADDR=${VAULT_URL}",
                 "VAULT_SKIP_VERIFY=true"]) {
            try {
                sh "vault login -method=ldap -no-print username=${VAULT_RO_USER} password=${VAULT_RO_PASSWORD}"
            } catch (e) {
                log.error("Error! Got an error trying to initiate the connect with Vault")
                error("Error! Got an error trying to initiate the connect with Vault ${VAULT_URL}")
            }
        }
    }
}

def kubectlInstall() {
    try {
        log.info("Ensure that kubectl installed")
        sh "kubectl version --client=true"
    } catch (e) {
        log.info("Going to install latest stable kubectl")
        sh """
            curl -LO https://storage.googleapis.com/kubernetes-release/release/\$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
            chmod +x ./kubectl
            ./kubectl version --client=true
           """
    }
}


def vaultInstall() {
    try {
        log.info("Ensure that vault is installed")
        sh "command vault -v"
    } catch (e) {
        log.info("Going to install vault client 1.1.0 version")
        sh """
            wget -O vault.zip https://releases.hashicorp.com/vault/1.1.0/vault_1.1.0_linux_amd64.zip
            unzip vault.zip
            ./vault -v
           """
    }
}

def kubeup(String serviceName, String configSet, String nameSpace = '', Boolean dryRun = false) {
    String dryRunParam = dryRun ? '--dry-run' : ''
    String nameSpaceParam = nameSpace == '' ? '' : "--namespace ${nameSpace}"

    sh """
       unset KUBERNETES_SERVICE_HOST
       ./kubeup --yes --no-color ${dryRunParam} ${nameSpaceParam} --configset ${configSet} ${serviceName} 2>&1
    """
}

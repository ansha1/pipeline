import static com.nextiva.SharedJobsStaticVars.*


def deploy(String serviceName, String buildVersion, String clusterDomain, List kubernetesDeploymentsList, String nameSpace = 'default') {

    def envName = "${clusterDomain.tokenize('.').get(0)}"
    def configSet = "aws-${envName}"
    log.info("Choosen configSet is ${configSet} for ${clusterDomain}")

    withEnv(["BUILD_VERSION=${buildVersion.replace('+', '-')}",
             "KUBELOGIN_CONFIG=${env.WORKSPACE}/.kubelogin",
             "KUBECONFIG=${env.WORKSPACE}/kubeconfig",
             "PATH=${env.PATH}:${WORKSPACE}"]) {

        kubectlInstall()
        vaultInstall()
        jqInstall()
        kubedogInstall()

        try {
            login(clusterDomain)
            vaultLogin()

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
                if(!it.contains('/')) {
                    it = "deployment/${it}"
                }
                sh """
                   unset KUBERNETES_SERVICE_HOST
                   kubectl rollout status ${it} --namespace ${nameSpace}
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

    withCredentials([usernamePassword(credentialsId: 'jenkinsbitbucket', usernameVariable: 'KUBELOGIN_USERNAME', passwordVariable: 'KUBELOGIN_PASSWORD')]) {
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
        pythonUtils.venvSh("""
            pip3 install -U wheel
            pip3 install http://repository.nextiva.xyz/repository/pypi-dev/packages/nextiva-kubelogin/${kubelogin_version}/nextiva-kubelogin-${kubelogin_version}.tar.gz
        """, false, k8sEnv)

        pythonUtils.venvSh("""
            unset KUBERNETES_SERVICE_HOST
            kubelogin -s login.${clusterDomain} 2>&1
            kubectl get nodes
            """, false, k8sEnv)
    }
}

def vaultLogin() {
    withCredentials([usernamePassword(credentialsId: "vault-ro-access", usernameVariable: 'VAULT_RO_USER', passwordVariable: 'VAULT_RO_PASSWORD')]) {
        // vault login
        try {
            sh "vault login -method=ldap -no-print -address ${VAULT_URL} username=${VAULT_RO_USER} password=${VAULT_RO_PASSWORD}"
        } catch (e) {
            log.error("Error! Got an error trying to initiate the connect with Vault")
            error("Error! Got an error trying to initiate the connect with Vault ${VAULT_URL}")
        }
    }
}

def kubectlInstall() {
    try {
        log.info("Ensure that kubectl is nstalled")
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
            unzip -o vault.zip
            ./vault -v
           """
    }
}

def jqInstall() {
    try {
        log.info("Ensure that jq is installed")
        sh "jq --version"
    } catch (e) {
        log.info("Going to install latest stable jq")
        sh """
            curl -LO https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64
            mv jq-linux64 jq
            chmod +x ./jq
            ./jq --version
           """
    }
}

def kubedogInstall() {
    log.debug("kubedogInstall start")
    try {
        String output = common.shWithOutput("kubedog version")
        log.debug("$output")
    } catch (e) {
        log.warn("kubedog is not installed, going to install kubedog...")
        String out = common.shWithOutput("""
            curl -L https://dl.bintray.com/flant/kubedog/v${KUBEDOG_VERSION}/kubedog-linux-amd64-v${KUBEDOG_VERSION} -o ./kubedog
            chmod +x ./kubedog
            ./kubedog version""")
        log.debug("$out")
    }
    log.debug("kubedogInstall complete")
}

def kubeup(String serviceName, String configSet, String nameSpace = '', Boolean dryRun = false) {
    log.debug("Deploy application: ${serviceName}, namespace: ${nameSpace}, configset: ${configSet}, dryRun = ${dryRun}")

    String dryRunParam = dryRun ? '--dry-run' : ''
    String nameSpaceParam = nameSpace == '' ? '' : "--namespace ${nameSpace}"

    String kubeupEnv = ".venv_kubeup_${common.getRandomInt()}"
    String kubeupOutputFile = "kubeup_output_${common.getRandomInt()}.txt"
    pythonUtils.createVirtualEnv("python3", kubeupEnv)

    // install kubeup
    pythonUtils.venvSh("""
        ${unsetEnvServiceDiscovery()}

        pip3 install -U wheel
        pip3 install http://repository.nextiva.xyz/repository/pypi-dev/packages/nextiva-kubeup/${KUBEUP_VERSION}/nextiva-kubeup-${KUBEUP_VERSION}.tar.gz
        kubeup -v
        """, false, kubeupEnv)

    // deploy app
    String output = pythonUtils.venvSh("""
        ${unsetEnvServiceDiscovery()}

        kubeup --yes --no-color ${dryRunParam} ${nameSpaceParam} --configset ${configSet} ${serviceName} 2>&1 | tee ${kubeupOutputFile}
        """, false, kubeupEnv)

    def kubeupOutput = readFile kubeupOutputFile

    if (!dryRun) {
        sleep 15 // add sleep to avoid failures when deployment doesn't exist yet PIPELINE-93
        validate(kubeupOutput, nameSpace)
    }
}

def validate(String installOutput, String namespace) {
    log.debug("find all kubernetes objects in the cloudapp in order to validate", installOutput)
    log.debug("==========================================================================================")

    List objectsToValidate = []
    installOutput.split("\n").each {
        log.debug("parse object $it")
        switch (it) {
            case ~/^(deployment.apps|javaapp.nextiva.io|pythonapp.nextiva.io).+$/:
                log.warn("Found k8s object $it")
                objectsToValidate.add("deployment ${extractObject(it)}")
                break
            case ~/^statefulset.apps.+$/:
                log.warn("Found k8s object $it")
                objectsToValidate.add("statefulset ${extractObject(it)}")
                break
            case ~/^daemonset.extentions.+$/:
                log.warn("Found k8s object $it")
                objectsToValidate.add("daemonset ${extractObject(it)}")
                break
            case ~/^job.batch.+$/:
                log.warn("Found k8s object $it")
                objectsToValidate.add("job ${extractObject(it)}")
                break
        }
    }
    log.debug("Collected objectsToValidate", objectsToValidate)
    objectsToValidate.each {
        sh "kubedog -n ${namespace} rollout track ${it} 2>&1"
    }
}

String extractObject(String rawString) {
    log.debug("got string", rawString)
    String extractedObject = rawString.substring(rawString.indexOf("/") + 1, rawString.indexOf(" "))
    log.debug("extractedObject", extractedObject)
    return extractedObject
}

String unsetEnvServiceDiscovery() {
    // fix for builds running in kubernetes, clean up predefined variables.

    String envsToUnset = ""
    String currentEnv = common.shWithOutput("printenv")
    currentEnv.split("\n").findAll { it ==~ ~/.+(_SERVICE_|_PORT).+/ }.each {
        envsToUnset += "unset ${it.tokenize("=")[0]}\n"
    }
    log.debug("envsToUnset:\n $envsToUnset")
    return envsToUnset
}

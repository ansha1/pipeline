import static com.nextiva.SharedJobsStaticVars.*


def deploy(String serviceName, String buildVersion, String clusterDomain, List kubernetesDeploymentsList, String nameSpace = 'default', Boolean verify = false) {

    def envName = "${clusterDomain.tokenize('.').get(0)}"
    def configSet = "aws-${envName}"

    log.info("Choosen configSet is ${configSet} for ${clusterDomain}")

    String extraParams = ""
    String k8sEnv = '.k8env'
    if (verify) {
        extraParams = "-v"
    }

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

    withCredentials([usernamePassword(credentialsId: 'jenkinsbitbucket', usernameVariable: 'KUBELOGIN_USERNAME', passwordVariable: 'KUBELOGIN_PASSWORD')]) {
        withEnv(["BUILD_VERSION=${buildVersion.replace('+', '-')}",
                 "KUBELOGIN_CONFIG=${env.WORKSPACE}/.kubelogin",
                 "KUBECONFIG=${env.WORKSPACE}/kubeconfig",
                 "PATH=${env.PATH}:${WORKSPACE}"]) {

            def repoDir = prepareRepoDir(KUBERNETES_REPO_URL, KUBERNETES_REPO_BRANCH)
            def kubelogin_version = KUBERNETES_KUBELOGIN_DEFAULT_VERSION

            try {
                def response = httpRequest quiet: !log.isDebug(), consoleLogResponseBody: log.isDebug(),
                        url: "https://login.${clusterDomain}/info"
                def responseJson = readJSON text: response.content

                if (responseJson.data.build_version) {
                    kubelogin_version = responseJson.data.build_version.replace('-', '+')
                }

                log.info("Going to install kubelogin (${kubelogin_version})")
                pythonUtils.createVirtualEnv("python3", k8sEnv)
                pythonUtils.venvSh("pip3 install http://repository.nextiva.xyz/repository/pypi-dev/packages/nextiva-kubelogin/${kubelogin_version}/nextiva-kubelogin-${kubelogin_version}.tar.gz", false, k8sEnv)
                sh """
                    unset KUBERNETES_SERVICE_HOST
                    ${k8sEnv}/bin/kubelogin -s login.${clusterDomain}
                    kubectl get nodes
                    echo 'Checking of application manifests ...'
                    ${repoDir}/kubeup ${extraParams} --dry-run --yes --no-color --namespace ${nameSpace} --configset ${configSet} ${serviceName}
                    echo 'Deploying application into Kubernetes ...'
                    ${repoDir}/kubeup ${extraParams} --yes --no-color --namespace ${nameSpace} --configset ${configSet} ${serviceName}
                    """
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
}

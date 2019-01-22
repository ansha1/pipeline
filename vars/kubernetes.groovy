import static com.nextiva.SharedJobsStaticVars.*


def deploy(String serviceName, String nameSpace, String clusterDomain, String buildVersion, verify = false) {

    def configSet = "aws-${clusterDomain.tokenize('.').get(0)}"

    log.info("Choosen configSet is ${configSet} for clusterDomain ${clusterDomain}")

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
            export PATH=\$PATH:${WORKSPACE}
            kubectl version --client=true
           """
    }

    withCredentials([usernamePassword(credentialsId: 'jenkinsbitbucket', usernameVariable: 'KUBELOGIN_USERNAME', passwordVariable: 'KUBELOGIN_PASSWORD')]) {
        withEnv(["BUILD_VERSION=${buildVersion.replace('+', '-')}",
                 "KUBELOGIN_CONFIG=${env.WORKSPACE}/.kubelogin"],
                 "KUBECONFIG=${env.WORKSPACE}/kubeconfig",
                 "PATH=${env.PATH}:${WORKSPACE}") {
            def repoDir = prepareRepoDir(KUBERNETES_REPO_URL, KUBERNETES_REPO_BRANCH)
            try {
                pythonUtils.createVirtualEnv("python3", k8sEnv)
                pythonUtils.venvSh("pip3 install http://repository.nextiva.xyz/repository/pypi-dev/packages/nextiva-kubelogin/${KUBERNETES_KUBELOGIN_VERSION}/nextiva-kubelogin-${KUBERNETES_KUBELOGIN_VERSION}.tar.gz", false, k8sEnv)
                sh """
                        unset KUBERNETES_SERVICE_HOST
                        ${k8sEnv}/bin/kubelogin -s login.${clusterDomain}
                        kubectl get nodes
                        ${repoDir}/kubeup ${extraParams} --yes --namespace ${nameSpace} --configset ${configSet} ${serviceName}
                    """
                log.info("Deploy to the Kubernetes cluster has been completed.")
            } catch (e) {
                log.warning("Deploy to the Kubernetes failed!")
                log.warning(e)
                error("Deploy to the Kubernetes failed! $e")
            }
            sleep 15 // add sleep to avoid failures when deployment doesn't exist yet PIPELINE-93
            try {
                sh """
                        export PATH=\$PATH:${WORKSPACE}
                        export KUBECONFIG="${env.WORKSPACE}/kubeconfig"
                        unset KUBERNETES_SERVICE_HOST
                        kubectl rollout status deployment/${serviceName} -n ${nameSpace}
                    """
            } catch (e) {
                log.warning("kubectl rollout status is failed!")
                log.warning("Ensure that your APP_NAME variable in the Jenkinsfile and metadata.name in app-operator manifest are the same")
                log.warning(e)
                currentBuild.rawBuild.result = Result.UNSTABLE
            }
        }
    }
}

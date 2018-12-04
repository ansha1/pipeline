import static com.nextiva.SharedJobsStaticVars.*


def deploy(String serviceName, String nameSpace, String clusterDomain, String configSet, String buildVersion, verify = false) {

    String extraParams = ""
    if (verify) {
        extraParams = "-v"
    }

    withCredentials([usernamePassword(credentialsId: 'jenkinsbitbucket', usernameVariable: 'KUBELOGIN_USERNAME', passwordVariable: 'KUBELOGIN_PASSWORD')]) {
        withEnv(["BUILD_VERSION=${buildVersion}"]) {
            def repoDir = prepareRepoDir(KUBERNETES_REPO_URL, KUBERNETES_REPO_BRANCH)
            try {
                pythonUtils.createVirtualEnv("python3.6")
                pythonUtils.venvSh("pip3 install http://repository.nextiva.xyz/repository/pypi-dev/packages/nextiva-kubelogin/${KUBERNETES_KUBELOGIN_VERSION}/nextiva-kubelogin-${KUBERNETES_KUBELOGIN_VERSION}.tar.gz")
                sh """
                        export KUBECONFIG="${env.WORKSPACE}/kubeconfig"
                        .env/bin/kubelogin -s login.${clusterDomain}
                        kubectl get nodes
                        ${repoDir}/kubeup ${extraParams} --yes --namespace ${nameSpace} --configset ${configSet} ${serviceName}
                        kubectl rollout status deployment/${serviceName} -n ${nameSpace}
                    """
                log.info("Deploy to the Kubernetes cluster has been completed.")
            } catch (e) {
                log.warning("Deploy to the Kubernetes failed!")
                log.warning(e)
                error("Deploy to the Kubernetes failed! $e")
            }
        }
    }
}

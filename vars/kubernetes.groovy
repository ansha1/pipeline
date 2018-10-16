import static com.nextiva.SharedJobsStaticVars.*


def deploy(String serviceName, String nameSpace, String clusterDomain, String configSet, String buildVersion, verify = false, nodeLabel = KUBERNETES_NODE_LABEL) {

    String extraParams = ""
    if (verify) {
        extraParams = "-v"
    }

    node(nodeLabel) {
        withCredentials([usernamePassword(credentialsId: 'jenkinsbitbucket', usernameVariable: 'KUBELOGIN_USERNAME', passwordVariable: 'KUBELOGIN_PASSWORD')]) {
            withEnv(["BUILD_VERSION=${buildVersion}"]) {
                def repoDir = prepareRepoDir(KUBERNETES_REPO_URL, KUBERNETES_REPO_BRANCH)
                try {
                        pythonUtils.createVirtualEnv("python3.6")
                        pythonUtils.venvSh("pip3 install http://repository.nextiva.xyz/repository/pypi-dev/packages/nextiva-kubelogin/0.3.5/nextiva-kubelogin-0.3.5.tar.gz")
                        sh """export KUBECONFIG="${env.WORKSPACE}/kubeconfig"
                              .env/bin/kubelogin -s login.${clusterDomain}
                              kubectl get nodes
                              ${repoDir}/kubeup ${extraParams} -f -n ${nameSpace} -c ${configSet} ${serviceName}"""

                    log.info("Deploy to the Kubernetes cluster has been completed.")


                } catch (e) {
                    log.warning("Deploy to the Kubernetes failed!")
                    log.warning(e)
                    error("Deploy to the Kubernetes failed! $e")
                }
            }
        }
    }
}

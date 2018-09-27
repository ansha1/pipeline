import static com.nextiva.SharedJobsStaticVars.*


def deploy(String serviceName, String nameSpace, String clusterDomain, String configSet, String buildVersion, verify = false, nodeLabel = KUBERNETES_NODE_LABEL) {

    String extraParams = ""
    if (verify) {
        extraParams = "-v"
    }

    node(nodeLabel) {
        sh "docker pull repository.nextiva.xyz/kubelogin"
        withCredentials([usernamePassword(credentialsId: 'jenkinsbitbucket', usernameVariable: 'KUBELOGIN_USERNAME', passwordVariable: 'KUBELOGIN_PASSWORD')]) {
            withEnv(["BUILD_VERSION=${buildVersion}"]) {
                def repoDir = prepareRepoDir(KUBERNETES_REPO_URL, KUBERNETES_REPO_BRANCH)
                try {
                    docker.image('repository.nextiva.xyz/kubelogin').inside("-v /etc/passwd:/etc/passwd:ro -v $repoDir:$repoDir:rw") {
                        sh """KUBECONFIG = "${env.WORKSPACE}/kubeconfig"
                              kubelogin -s login.${clusterDomain}
                              ${repoDir}/kubeup ${extraParams} -f -n ${nameSpace} -c ${configSet} ${serviceName}"""
                    }
                    log.info("Deploy to the Kubernetes cluster has been completed.")


                } catch (e) {
                    log.warning("Deploy to the Kubernetes failed!")
                    log.warning(e)
                }
            }
        }
    }
}

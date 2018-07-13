import static com.nextiva.SharedJobsStaticVars.*


def deploy(String serviceName, String nameSpace, String configSet, String buildVersion, verify=false, nodeLabel=KUBERNETES_NODE_LABEL) {

    String extraParams = ""
    if(verify) {
        extraParams = "-v"
    }

    node(nodeLabel) {
        def repoDir = prepareRepoDir(KUBERNETES_REPO_URL, KUBERNETES_REPO_BRANCH)
        try {
            withEnv(["BUILD_VERSION=${buildVersion}"]) {
                sh "/bin/bash ${repoDir}/kubeup ${extraParams} -f -n ${nameSpace} -c ${configSet} ${serviceName}"
            }            
            log.info("Deploy to Kubernetes namespace ${nameSpace} has been complited.")
        } catch (e) {
            log.warning("Deploy to Kubernetes failed!")
            log.warning(e)
        }
    }
}

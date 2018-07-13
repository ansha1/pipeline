import static com.nextiva.SharedJobsStaticVars.*


def deploy(String serviceName, String nameSpace, String configSet, String buildVersion, verify=false, nodeLabel=KUBERNETES_NODE_LABEL) {

    String extraParams = ""
    if(verify) {
        extraParams = "-v"
    }

    node(nodeLabel) {
        def repoDir = prepareRepoDir.call(KUBERNETES_REPO_URL, KUBERNETES_REPO_BRANCH)
        try {
            withEnv(["BUILD_VERSION=${buildVersion}"]) {
                sh "/bin/bash ${repoDir}/kubeup ${extraParams} -f -n ${nameSpace} -c ${configSet} ${serviceName}"
            }            
            echo "Deploy to Kubernetes namespace ${nameSpace} has been complited."
        } catch (e) {
            echo "\nERROR: Deploy to Kubernetes failed!"
            echo "${e}\n"
        }
    }
}

import io.fabric8.kubernetes.client.KubernetesClient
import org.csanchez.jenkins.plugins.kubernetes.*

def call(Map slaveConfig, body) {

    //Always generating namespace from JOB_NAME
    def namespaceName = getNamespaceNameFromString(JOB_NAME)
    def slaveName = slaveConfig.get("slaveName", "slave")
    def image = slaveConfig.get("image")
    if (image == null) {
        error "Slave image is not defined, please define it in the your slaveConfig"
    }
    def resourceRequestCpu = slaveConfig.get("resourceRequestCpu", "250m")
    def resourceRequestMemory = slaveConfig.get("resourceRequestMemory", "1Gi")
    def buildDaysToKeepStr = slaveConfig.get("buildDaysToKeepStr", "3")
    def buildNumToKeepStr = slaveConfig.get("buildNumToKeepStr", "5")
    def jobTimeoutMinutes = slaveConfig.get("jobTimeoutMinutes", "60")
    def paramlist = slaveConfig.get("paramlist", []) + [booleanParam(name: 'DEBUG', description: 'Enable DEBUG mode with extended output', defaultValue: false)]

    /*jobTriggers could be
    cron('H/15 * * * *')
    upstream(threshold: hudson.model.Result.SUCCESS, upstreamProjects: "surveys-server/dev")
    */
    def jobTriggers = slaveConfig.get("jobTriggers", [])
    def authMap = slaveConfig.get("auth", [:])
    def allowedUsers = authMap.get(env.BRANCH_NAME, ["authenticated"])
    def securityPermissions = generateSecurityPermissions(allowedUsers)
    def propertiesList = [parameters(paramlist),
                          buildDiscarder(logRotator(daysToKeepStr: buildDaysToKeepStr, numToKeepStr: buildNumToKeepStr)),
                          authorizationMatrix(inheritanceStrategy: nonInheriting(), permissions: securityPermissions),
                          disableConcurrentBuilds(),
                          pipelineTriggers(jobTriggers)]

    def label = "${slaveName}-${UUID.randomUUID().toString()}"
    def parentPodtemplateYaml = libraryResource 'podtemplate/default.yaml'

    //Executing every Jenkins slave in the dedicated namespace
    withNamespace(namespaceName) {
        podTemplate(label: label, yaml: parentPodtemplateYaml, workingDir: '/home/jenkins', namespace: namespaceName,
                containers: [
                        containerTemplate(name: 'jnlp', image: "jenkinsci/jnlp-slave:3.27-1-alpine", args: '${computer.jnlpmac} ${computer.name}'),
                        containerTemplate(name: 'build', image: image, command: 'cat', ttyEnabled: true,
                                resourceRequestCpu: resourceRequestCpu,
                                resourceRequestMemory: resourceRequestMemory,
                                envVars: [
                                        envVar(key: 'CYPRESS_CACHE_FOLDER', value: '/opt/cypress_cache'),
                                        envVar(key: 'YARN_CACHE_FOLDER', value: '/opt/yarn_cache'),
                                        envVar(key: 'CYPRESS_CACHE_FOLDER', value: '/opt/cypress_cache'),
                                        envVar(key: 'npm_config_cache', value: '/opt/npmcache'),
                                        envVar(key: 'M2_LOCAL_REPO', value: '/home/jenkins/.m2repo')
                                ],)],
                volumes: [hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
                          hostPathVolume(hostPath: '/opt/m2cache', mountPath: '/home/jenkins/.m2repo'),
                          hostPathVolume(hostPath: '/opt/npmcache', mountPath: '/opt/npmcache'),
                          hostPathVolume(hostPath: '/opt/cypress_cache', mountPath: '/opt/cypress_cache'),
                          hostPathVolume(hostPath: '/opt/yarncache', mountPath: '/opt/yarncache'),
//                          secretVolume(mountPath: '/root/.m2', secretName: 'maven-secret')]) {  //TODO: add this secret as shared secret file in all k8s namespaces
                ]) {

            timestamps {
                ansiColor('xterm') {
                    timeout(time: jobTimeoutMinutes, unit: 'MINUTES') {
                        node(label) {
                            properties(propertiesList)
                            body.call()
                        }
                    }
                }
            }
        }
    }
}


List<String> generateSecurityPermissions(List<String> allowedUsers) {
    List<String> basicList = ['hudson.model.Item.Read:authenticated']
    allowedUsers.each {
        basicList.add("hudson.model.Item.Build:${it}")
        basicList.add("hudson.model.Item.Cancel:${it}")
        basicList.add("hudson.model.Item.Workspace:${it}")
    }
    return basicList
}

/*
This method allow us to run pods in the dedicated namespace.
The namespace will be deleted after execution
 */
def withNamespace(String namespaceName, body) {
    try {
        def k8sClient = KubernetesClientProvider.createClient(Jenkins.instance.clouds.get(0))
        String ns = k8sClient.namespaces().createNew().withNewMetadata().withName(namespaceName).endMetadata().done()
        log.info("Created namespace ${ns}")
        k8sClient.close()
        k8sClient = null

        body()  //execute closure body
    } catch (e) {
        log.error("There is error in withNamespace method ${e}")
    } finally {
        k8sClient = KubernetesClientProvider.createClient(Jenkins.instance.clouds.get(0))
        String isNamespaceDeleted = k8sClient.namespaces().withName(namespaceName).delete()
        log.info("Deleted namespace ${namespaceName} ${isNamespaceDeleted}")
        k8sClient.close()  //always close connection to the Kubernetes cluster to prevent connection leaks
        //if we don't null client, jenkins will try to serialise k8s objects and that will fail, so we won't see actual error
        k8sClient = null
    }
}

String getNamespaceNameFromString(String rawNamespaceName) {
    //By convention, the names of Kubernetes resources should be up to maximum length of 253 characters and consist of lower case alphanumeric characters, -
    return rawNamespaceName.trim().replaceAll('[^a-zA-Z\\d]', '-')
            .toLowerCase().take(253)
}
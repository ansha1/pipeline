import io.fabric8.kubernetes.client.KubernetesClient
import static com.nextiva.SharedJobsStaticVars.*
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
    def buildDaysToKeepStr = slaveConfig.get("buildDaysToKeepStr", "10")
    def buildNumToKeepStr = slaveConfig.get("buildNumToKeepStr", "10")
    def jobTimeoutMinutes = slaveConfig.get("jobTimeoutMinutes", "60")
    def paramlist = slaveConfig.get("paramlist", []) + [booleanParam(name: 'DEBUG', description: 'Enable DEBUG mode with extended output', defaultValue: false)]

    /*jobTriggers could be
    [cron('H/15 * * * *'),
    upstream(threshold: hudson.model.Result.SUCCESS, upstreamProjects: "surveys-server/dev")]
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
    def parentPodTemplateYaml = libraryResource 'podtemplate/default.yaml'

    //Executing every Jenkins slave in the dedicated namespace
    withNamespace(namespaceName) {
        podTemplate(label: "parent-$label", yaml: parentPodTemplateYaml) {
            podTemplate(label: label, workingDir: '/home/jenkins', namespace: namespaceName,
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
                              secretVolume(mountPath: '/root/.m2', secretName: 'maven-secret'),
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
        def ns = createNamespace(namespaceName)
        log.info("Created namespace ${ns}")
        body()  //execute closure body
    } catch (e) {
        log.error("There is error in withNamespace method ${e}")
    } finally {
        String isNamespaceDeleted = deleteNamespace(namespaceName)
        log.info("Deleted namespace ${namespaceName} ${isNamespaceDeleted}")
    }
}

String getNamespaceNameFromString(String rawNamespaceName) {
    //By convention, the names of Kubernetes resources should be up to maximum length of 253 characters and consist of lower case alphanumeric characters, -
    return rawNamespaceName.trim().replaceAll('[^a-zA-Z\\d]', '-')
            .toLowerCase().take(253)
}


@NonCPS
def getKubernetesClient() {
    return KubernetesClientProvider.createClient(Jenkins.instance.clouds.get(0))
}

@NonCPS
def createNamespace(String namespaceName) {
    List listOfBookedNamespaces = LIST_OF_BOOKED_NAMESPACES
    if (listOfBookedNamespaces.contains(namespaceName)) {
        error("Can't create ${namespaceName}, this namespace is already booked")
    }

    def kubernetesClient = getKubernetesClient()
    def namespace = kubernetesClient.namespaces().createNew().withNewMetadata().withName(namespaceName).endMetadata().done()
    kubernetesClient = null
    return namespace
}


@NonCPS
Boolean deleteNamespace(String namespaceName) {
    List listOfBookedNamespaces = LIST_OF_BOOKED_NAMESPACES
    if (listOfBookedNamespaces.contains(namespaceName)) {
        error("Can't delete ${namespaceName}, this namespace is already booked")
    }
    def kubernetesClient = getKubernetesClient()
    Boolean result = kubernetesClient.namespaces().withName(namespaceName).delete()
    kubernetesClient = null
    return result
}


@NonCPS
def createSecret(String namespaceName, String secret) {
    def kubernetesClient = getKubernetesClient()
//    def secretToApply = kubernetesClient.load(mavenSecret).get()
    kubernetesClient = null
}
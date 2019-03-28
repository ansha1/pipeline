import io.fabric8.kubernetes.client.KubernetesClient
import static com.nextiva.SharedJobsStaticVars.*
import org.csanchez.jenkins.plugins.kubernetes.*

def call(Map slaveConfig, body) {

    String iD = buildID(env.JOB_NAME, env.BUILD_NUMBER)
    String namespaceName = slaveConfig.get("namespace", iD)

    Map jobProperties = slaveConfig.get("jobProperties")
    if (!jobProperties) {
        jobWithProperties(jobProperties)
    }

    List<Map> containerResources = slaveConfig.get("containerResource")
    if (!containerResources) {
        error "ContainerResources is not defined, please define it in your slaveConfig: $slaveConfig"
    }

    def jobTimeoutMinutes = slaveConfig.get("jobTimeoutMinutes", "60")

    withNamespace(namespaceName) {
        def parentPodTemplateYaml = libraryResource 'podtemplate/default.yaml'
        podTemplate(label: "parent-$iD", yaml: parentPodTemplateYaml) {}

        podTemplate(label: iD, namespace: namespaceName, containers: containers(containerResources), volumes: volumes()) {
            timestamps {
                ansiColor('xterm') {
                    timeout(time: jobTimeoutMinutes, unit: 'MINUTES') {
                        node(label) {
                            body()
                        }
                    }
                }
            }
        }
    }
}


List containers(List<Map> containerResources) {
    List result
    containerResources.each { c -> result << containerInstance(c) }
    return result
}

def containerInstance(Map containerConfig) {
    def name = containerConfig.get("name")
    if (!name) {
        error("Cannot create container instance from config: \n $containerConfig \nThe name of container is undefined")
    }
    def image = containerConfig.get("image")
    if (!image) {
        error("Cannot create container instance from config: \n $containerConfig \nThe image of container is undefined")
    }
    def command = containerConfig.get("command", "cat")
    def ttyEnabled = containerConfig.get("ttyEnabled", true)
    def privileged = containerConfig.get("privileged", true)
    def alwaysPullImage = containerConfig.get("alwaysPullImage", true)
    def workingDir = containerConfig.get("workingDir", "/home/jenkins")
    def resourceRequestCpu = containerConfig.get("resourceRequestCpu", "50m")
    def resourceLimitCpu = containerConfig.get("resourceLimitCpu", "3000m")
    def resourceRequestMemory = containerConfig.get("resourceRequestMemory", "128Mi")
    def resourceLimitMemory = containerConfig.get("resourceLimitMemory", "6144Mi")
    def envVars = containerConfig.get("envVars", [:])

    return containerTemplate(name: name, image: image, command: command, ttyEnabled: ttyEnabled, privileged: privileged,
            alwaysPullImage: alwaysPullImage, workingDir: workingDir,
            resourceRequestCpu: resourceRequestCpu, resourceLimitCpu: resourceLimitCpu,
            resourceRequestMemory: resourceRequestMemory, resourceLimitMemory: resourceLimitMemory,
            envVars: processEnvVars(envVars))
}

def volumes() {
    [
            hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
            hostPathVolume(hostPath: '/opt/m2cache', mountPath: '/home/jenkins/.m2repo'),
            hostPathVolume(hostPath: '/opt/npmcache', mountPath: '/opt/npmcache'),
            hostPathVolume(hostPath: '/opt/cypress_cache', mountPath: '/opt/cypress_cache'),
            hostPathVolume(hostPath: '/opt/yarncache', mountPath: '/opt/yarncache'),
            secretVolume(mountPath: '/root/.m2', secretName: 'maven-secret'),
    ]
}

List processEnvVars(Map extraEnv) {
    envVars = [envVar(key: 'YARN_CACHE_FOLDER', value: '/opt/yarn_cache'),
               envVar(key: 'CYPRESS_CACHE_FOLDER', value: '/opt/cypress_cache'),
               envVar(key: 'npm_config_cache', value: '/opt/npmcache'),
               envVar(key: 'M2_LOCAL_REPO', value: '/home/jenkins/.m2repo')]
    extraEnv.each { e -> envVars << envVar(key: "${e.key}", value: "${e.value}")
    }
    return envVars
}

/**
 * This method allow us to run pods in the dedicated namespace.
 * The namespace will be deleted after execution
 * @param namespaceName usually buildID
 * @param body Closure which will be executed
 * */
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


@NonCPS
def getKubernetesClient() {
    return KubernetesClientProvider.createClient(Jenkins.instance.clouds.get(0))
}

@NonCPS
def createNamespace(String namespaceName) {
    List listOfBookedNamespaces = LIST_OF_BOOKED_NAMESPACES
    if (listOfBookedNamespaces.contains(namespaceName)) {
        log.info("Running build in the already created namespace")
        return true
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
        log.info("Namespace ${namespaceName} can't be deleted because it is perisitent")
        return false
    }
    def kubernetesClient = getKubernetesClient()
    Boolean result = kubernetesClient.namespaces().withName(namespaceName).delete()
    kubernetesClient = null
    return result
}
import com.cloudbees.groovy.cps.NonCPS
import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.client.KubernetesClient
import static com.nextiva.SharedJobsStaticVars.*
import static com.nextiva.Utils.buildID
import org.csanchez.jenkins.plugins.kubernetes.*

def call(Map slaveConfig, body) {
    echo("slaveConfig  kubeslave $slaveConfig")

    String iD = buildID(env.JOB_NAME, env.BUILD_NUMBER)

    if (slaveConfig.containsKey("jobProperties")) {
        jobWithProperties(slaveConfig.get("jobProperties"))
    }

    Map<String, Map> containerResources = slaveConfig.get("containerResources")
    if (!containerResources) {
        error "ContainerResources is not defined, please define it in your slaveConfig: $slaveConfig"
    }

    withNamespace(iD) {
        def parentPodTemplateYaml = libraryResource 'podtemplate/default.yaml'
        podTemplate(label: "parent-$iD", showRawYaml: false, yaml: parentPodTemplateYaml) {}

        podTemplate(label: iD, namespace: iD, showRawYaml: false, inheritFrom: "parent-$iD", containers: containers(containerResources), volumes: volumes()) {
            timestamps {
                ansiColor('xterm') {
                    node(iD) {
                        body()
                    }
                }
            }
        }
    }
}


List containers(Map<String, Map> containerResources) {
    List result = []
    containerResources.each { k, v ->
        v.put("name", k)
        result << containerInstance(v)
    }
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
        currentBuild.result = "FAILED"
        log.error("There is error in withNamespace method ${e}:  ${e.stackTrace}")
    } finally {
        String isNamespaceDeleted = deleteNamespace(namespaceName)
        log.info("Deleted namespace ${namespaceName} ${isNamespaceDeleted}")
    }
}


@NonCPS
KubernetesClient getKubernetesClient() {
    return KubernetesClientProvider.createClient(Jenkins.instance.clouds.get(0))
}

@NonCPS
def createNamespace(String namespaceName) {
    List listOfBookedNamespaces = LIST_OF_BOOKED_NAMESPACES
    if (listOfBookedNamespaces.contains(namespaceName)) {
        log.info("Running build in the already created namespace")
        return true
    }
    KubernetesClient kubernetesClient = getKubernetesClient()
    //Create namespace
    def namespace = kubernetesClient.namespaces().createNew().withNewMetadata().withName(namespaceName).endMetadata().done()
    log.info("created namespace")
    //Create mandatory secrets in the namespace
    def res1 = createResourceFromLibrary("kubernetes/maven-secret.yaml", "Secret", namespaceName)
    log.info("created mvnsecret $res1")
    def res2 = createResourceFromLibrary("kubernetes/regsecret.yaml", "Secret", namespaceName)
    log.info("created regsecret $res2")

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
    KubernetesClient kubernetesClient = getKubernetesClient()
    Boolean result = kubernetesClient.namespaces().withName(namespaceName).delete()
    kubernetesClient = null
    return result
}

@NonCPS
def createResourceFromLibrary(String resourcePath, String kind, String namespaceName) {
    log.info("1")
    String libraryResource = libraryResource(resourcePath)
    log.info("libraryResource:$libraryResource")
    KubernetesClient kubernetesClient = getKubernetesClient()
    switch (kind) {
        case "Secret":
            Secret secret = kubernetesClient.secrets().load(new ByteArrayInputStream(libraryResource.getBytes())).get()
            secret.metadata.namespace = namespaceName
            def result = kubernetesClient.secrets().inNamespace("namespaceName").create(secret)
            kubernetesClient = null
            log.info("created resource $result")
            return result
            break
        default:
            kubernetesClient = null
            error("This resource kind: $kind, resourcePath: $resourcePath is not supported")
    }
}
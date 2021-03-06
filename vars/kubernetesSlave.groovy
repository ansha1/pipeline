import com.cloudbees.groovy.cps.NonCPS
import com.nextiva.utils.Logger
import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.client.KubernetesClient
import jenkins.model.Jenkins
import org.csanchez.jenkins.plugins.kubernetes.KubernetesClientProvider

import static com.nextiva.SharedJobsStaticVars.LIST_OF_BOOKED_NAMESPACES
import static com.nextiva.utils.Utils.buildID

def call(Map slaveConfig, body) {
    Logger logger = new Logger(this)
    logger.debug("Got slaveConfig", slaveConfig)

    String iD = buildID(env.JOB_NAME, env.BUILD_NUMBER)

    if (slaveConfig.containsKey("jobProperties")) {
        jobWithProperties(slaveConfig.get("jobProperties"))
    }

    Map<String, Map> containerResources = slaveConfig.get("containerResources")
    if (!containerResources) {
        error "ContainerResources is not defined, please define it in your slaveConfig: $slaveConfig"
    }

    def rawYaml = slaveConfig.get("rawYaml", """\
        spec:
          securityContext:
            runAsUser: 1000
            runAsGroup: 1000
            fsGroup: 1000
          tolerations:
          - key: tooling.nextiva.io
            operator: Equal
            value: jenkins
            effect: NoSchedule
    """.stripIndent())
    withNamespace(iD) {
        podTemplate(label: iD, namespace: iD, showRawYaml: false, slaveConnectTimeout: 1200, activeDeadlineSeconds: 1200,
                nodeSelector: 'dedicatedgroup=jenkins-slave', imagePullSecrets: ['regsecret'],
                annotations: [podAnnotation(key: 'cluster-autoscaler.kubernetes.io/safe-to-evict', value: 'false')],
                containers: containers(containerResources), volumes: volumes(), yaml: rawYaml) {
            node(iD) {
                body()
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
            hostPathVolume(hostPath: '/opt/shared_repos', mountPath: 'opt/shared_repos'),
            hostPathVolume(hostPath: '/opt/m2cache', mountPath: '/root/.m2repo'),
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
               envVar(key: 'M2_LOCAL_REPO', value: '/root/.m2repo'),
               envVar(key: 'MAVEN_CONFIG', value: '/root/.m2repo'),
               envVar(key: 'MAVEN_OPTS', value: '-Duser.home=/root')]
    extraEnv.each { e -> envVars << envVar(key: "${e.key}", value: "${e.value}") }
    return envVars
}

/**
 * This method allow us to run pods in the dedicated namespace.
 * The namespace will be deleted after execution
 * @param namespaceName usually buildID
 * @param body Closure which will be executed
 * */
def withNamespace(String namespaceName, body) {
    Logger logger = new Logger(this)
    try {
        def ns = createNamespace(namespaceName)
        logger.trace("Created namespace ${ns}")
        body()  //execute closure body
    } catch (e) {
        currentBuild.result = "FAILURE"
        logger.error("There is error in withNamespace method ${e}:  ${e.stackTrace}")
    } finally {
        String isNamespaceDeleted = deleteNamespace(namespaceName)
        logger.trace("Deleted namespace ${namespaceName} ${isNamespaceDeleted}")
    }
}


@NonCPS
KubernetesClient getKubernetesClient() {
    return KubernetesClientProvider.createClient(Jenkins.get().clouds.get(0))
}

def createNamespace(String namespaceName) {
    Logger logger = new Logger(this)
    List listOfBookedNamespaces = LIST_OF_BOOKED_NAMESPACES
    if (listOfBookedNamespaces.contains(namespaceName)) {
        logger.info("Running build in the already created namespace")
        return true
    }
    KubernetesClient kubernetesClient = getKubernetesClient()
    //Create namespace
    def namespace = kubernetesClient.namespaces().createNew().withNewMetadata().withName(namespaceName).endMetadata().done()
    logger.trace("created namespace: $namespace")
    //Create mandatory secrets in the namespace
    def mvnSecret = createResourceFromLibrary("kubernetes/maven-secret.yaml", "Secret", namespaceName)
    logger.trace("created resource  $mvnSecret")
    def regSecret = createResourceFromLibrary("kubernetes/regsecret.yaml", "Secret", namespaceName)
    logger.trace("created resource $regSecret")

    kubernetesClient = null
    return namespace
}

Boolean deleteNamespace(String namespaceName) {
    Logger logger = new Logger(this)
    List listOfBookedNamespaces = LIST_OF_BOOKED_NAMESPACES
    if (listOfBookedNamespaces.contains(namespaceName)) {
        logger.info("Namespace ${namespaceName} can't be deleted because it is persistent")
        return false
    }
    KubernetesClient kubernetesClient = getKubernetesClient()
    Boolean result = kubernetesClient.namespaces().withName(namespaceName).delete()
    kubernetesClient = null
    return result
}

def createResourceFromLibrary(String resourcePath, String kind, String namespaceName) {
    Logger logger = new Logger(this)
    logger.debug("Method createResourceFromLibrary, input: resourcePath:$resourcePath, kind: $kind, namespaceName: $namespaceName")
    String libraryResource = libraryResource resourcePath
    logger.trace("libraryResource:$libraryResource")
    KubernetesClient kubernetesClient = getKubernetesClient()
    switch (kind) {
        case "Secret":
            Secret secret = kubernetesClient.secrets().load(new ByteArrayInputStream(libraryResource.getBytes())).get()
            secret.metadata.namespace = namespaceName
            def result = kubernetesClient.secrets().inNamespace(namespaceName).create(secret)
            kubernetesClient = null
            logger.trace("created resource $result")
            return result
            break
        default:
            kubernetesClient = null
            error("This resource kind: $kind, resourcePath: $resourcePath is not supported")
    }
}
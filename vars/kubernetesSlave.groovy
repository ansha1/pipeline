import io.fabric8.kubernetes.client.KubernetesClient
import static com.nextiva.SharedJobsStaticVars.*
import org.csanchez.jenkins.plugins.kubernetes.*

def call(Map slaveConfig, body) {

    String iD = buildID(env.JOB_NAME, env.BUILD_NUMBER)

    Map jobProperties = slaveConfig.get("jobProperties")
    if (jobProperties != null) {
        withNodeProperties(jobProperties)
    }

    String namespaceName = slaveConfig.get("namespace", iD)
    String image = slaveConfig.get("image")
    if (image == null) {
        error "Slave image is not defined, please define it in the your slaveConfig"
    }

    def resourceRequestCpu = slaveConfig.get("resourceRequestCpu", "250m")
    def resourceRequestMemory = slaveConfig.get("resourceRequestMemory", "1Gi")
    def jobTimeoutMinutes = slaveConfig.get("jobTimeoutMinutes", "60")
    def extraEnv = slaveConfig.get("extraEnv", [:])


    withNamespace(namespaceName) {
        def parentPodTemplateYaml = libraryResource 'podtemplate/default.yaml'
        podTemplate(label: "parent-$iD", yaml: parentPodTemplateYaml) {}

        podTemplate(label: iD, workingDir: '/home/jenkins', namespace: namespaceName,
                containers: [
                        jnlpTemplate(),
                        slaveTemplate("build", image, resourceRequestCpu, resourceRequestMemory, extraEnv),
                ],
                volumes: volumes()
        ) {
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


def slaveTemplate(String name, String image, String resourceRequestCpu, String resourceRequestMemory, Map extraEnv) {
    return containerTemplate(name: name, image: image, command: 'cat', ttyEnabled: true,
            resourceRequestCpu: resourceRequestCpu,
            resourceRequestMemory: resourceRequestMemory,
            envVars: processEnvVars(extraEnv))
}

def jnlpTemplate() {
    def jnlpImage = 'jenkinsci/jnlp-slave:3.27-1-alpine'

    return containerTemplate(
            name: 'jnlp',
            image: "${jnlpImage}",
            args: '${computer.jnlpmac} ${computer.name}',
            resourceLimitMemory: '256Mi'
    )
}

def processEnvVars(Map extraEnv) {
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
        return namespace
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

/**
 * Returns the id of the build, which consists of the job name and build number.
 * By convention, the names of Kubernetes resources should be up to maximum length of 253 characters and consist of lower case alphanumeric characters, -
 * @param jobName usually env.JOB_NAME
 * @param buildNum usually env.BUILD_NUMBER
 */
static String buildID(String jobName, String buildNum) {
    return "${jobName}-${buildNum}".replaceAll('[^a-zA-Z\\d]', '-').take(253)
}
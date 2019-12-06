package com.nextiva.tools.deploy


import com.nextiva.environment.Environment
import hudson.AbortException

import static com.nextiva.SharedJobsStaticVars.VAULT_URL
import static com.nextiva.config.Config.instance as config
import static com.nextiva.utils.GitUtils.clone
import static com.nextiva.utils.Utils.shWithOutput

class Repository implements Serializable {
    String path
    String repository
    String branch

    Repository(String path, String repository, String branch) {
        this.path = path
        this.repository = repository
        this.branch = branch
    }
}

class Kubeup extends DeployTool implements Serializable {

    String kubeUpHome
    Repository cloudApps
    Repository cloudPlatform

    String name
    String toolHome
    Boolean initialized = false

    String repository
    String branch

    Kubeup(Map deployToolConfig) {
        super(deployToolConfig)
        this.name = deployToolConfig.get("name")
        this.toolHome = "deploy/${name}"
        this.repository = deployToolConfig.get("repository")
        this.branch = deployToolConfig.get("branch")
        this.kubeUpHome = "$toolHome/kubeup"
        this.cloudApps = new Repository("$toolHome/cloud-apps",
                deployToolConfig.get("cloudAppsRepository"),
                deployToolConfig.get("cloudAppsBranch"))
        this.cloudPlatform = new Repository("$toolHome/cloud-platform",
                deployToolConfig.get("cloudPlatformRepository"),
                deployToolConfig.get("cloudPlatformBranch"))
        logger.debug("Kubeup has been initialized")
    }

    @Override
    void deploy(Environment environment) {
        init(environment.kubernetesCluster)
        logger.info("Start deploy cloudApp: ${config.appName} , version: ${config.version}, namespace: " +
                "${environment.kubernetesNamespace}, configset: ${environment.kubernetesConfigSet}")

        logger.info('Checking of application manifests ...')
        install(config.appName, config.version, environment.kubernetesNamespace, environment.kubernetesConfigSet, true)
        logger.info('Deploying application into Kubernetes ...')
        install(config.appName, config.version, environment.kubernetesNamespace, environment.kubernetesConfigSet, false)
        println("this is kubernetes deployment" + toString())
    }

    void init(String clusterDomain) {
        config.script.echo "\n\n\n\n\nkubeup init \n\n\n\n\n"
        logger.debug("start init $name tool")
        logger.debug(this.toString())

        logger.debug("Clonning repository $cloudApps.repository branch $cloudApps.branch into $cloudApps.path")
        clone(config.script, cloudApps.repository, cloudApps.branch, cloudApps.path)
        logger.debug("clone complete")

        logger.debug("Clonning repository $cloudPlatform.repository branch $cloudPlatform.branch into $cloudPlatform.path")
        clone(config.script, cloudPlatform.repository, cloudPlatform.branch, cloudPlatform.path)
        logger.debug("clone complete")
        def home = toolHome
        config.script.container(name) {
            vaultLogin(VAULT_URL)
            kubeLogin(clusterDomain)
        }
        logger.debug("init complete")
        initialized = true
    }

    def kubeLogin(String clusterDomain) {
        config.script.env.KUBELOGIN_CONFIG = "${toolHome}/.kubelogin"
        config.script.env.KUBECONFIG = "${toolHome}/kubeconfig"
        config.script.env.KUBEDOG_KUBE_CONFIG = "${toolHome}/kubeconfig"
        def output = ""
        config.script.withCredentials([config.script.usernamePassword(credentialsId: 'jenkinsbitbucket', usernameVariable: 'KUBELOGIN_USERNAME', passwordVariable: 'KUBELOGIN_PASSWORD')]) {
            output = shWithOutput(config.script, """
            unset KUBERNETES_SERVICE_HOST
            kubelogin -s login.${clusterDomain} 2>&1
            kubectl get nodes 2>&1
            """)
        }
        logger.debug("Kubelogin output", output)
    }

    def vaultLogin(String vaultUrl) {
        logger.debug("trying to login into the Vault")
        String output
        config.script.withCredentials([config.script.usernamePassword(credentialsId: "vault-ro-access", usernameVariable: 'VAULT_RO_USER', passwordVariable: 'VAULT_RO_PASSWORD')]) {
            try {
                output = shWithOutput(config.script, "vault login -method=ldap -no-print -address ${vaultUrl} username=${config.script.env.VAULT_RO_USER} password=${config.script.env.VAULT_RO_PASSWORD}")
            } catch (e) {
                throw new AbortException("Error! Got an error trying to initiate the connect with Vault ${vaultUrl}; output: $output; e: $e")
            }
        }
        logger.trace("Vault login output", output)
        logger.debug("Vault login complete")
    }

    def install(String application, String version, String namespace, String configset, Boolean dryRun = true) {
        logger.debug("Install application: $application, version: $version, namespace: $namespace, configset: $configset, dryRun = $dryRun")
        String output = ""
        def home = toolHome
        def cloudAppsPath = "./cloud-apps/apps/"
        def cloudPlatformPath = "./cloud-platform/apps"
        try {
            config.script.container(name) {
                config.script.dir(home) {
                    config.script.env.KUBELOGIN_CONFIG = "${config.script.env.WORKSPACE}/$home/.kubelogin"
                    config.script.env.KUBECONFIG = "${config.script.env.WORKSPACE}/$home/kubeconfig"
                    String dryRunParam = dryRun ? '--dry-run' : ''
                    output = shWithOutput(config.script, """
                    cd "\$(find $cloudAppsPath $cloudPlatformPath -maxdepth 1 -type d -name $application | head -1)/../../"
                    [ "\$PWD" = "/" ] && { echo '$application was not found'; exit 1; }
                    # fix for builds running in kubernetes, clean up predefined variables.
                    ${unsetEnvServiceDiscovery()}
                    BUILD_VERSION=$version
                    kubeup --yes --no-color $dryRunParam --namespace $namespace --configset $configset $application 2>&1
                    """)
                    if (!dryRun) {
                        validate(output, namespace)
                    }
                }
            }
        } catch (e) {
            throw new AbortException("kubeup install failure... e: $e\ninstallOutput: >>> \n $output")
        }
    }

    def validate(String installOutput, String namespace) {
        logger.debug("find all kubernetes objects in the cloudapp in order to validate", installOutput)
        logger.debug("==========================================================================================")

        List objectsToValidate = []
        installOutput.split("\n").each {
            logger.trace("parse object $it")
            if (it.contains(' created') || it.contains(' configured')) {
                switch (it) {
                    case ~/^(deployment.apps|javaapp.nextiva.io|pythonapp.nextiva.io).+$/:
                        logger.trace("Found k8s object $it")
                        objectsToValidate.add("deployment ${extractObject(it)}")
                        break
                    case ~/^statefulset.apps.+$/:
                        logger.trace("Found k8s object $it")
                        objectsToValidate.add("statefulset ${extractObject(it)}")
                        break
                    case ~/^daemonset.extentions.+$/:
                        logger.trace("Found k8s object $it")
                        objectsToValidate.add("daemonset ${extractObject(it)}")
                        break
                    case ~/^job.batch.+$/:
                        logger.trace("Found k8s object $it")
                        objectsToValidate.add("job ${extractObject(it)}")
                        break
                }
            }
        }
        logger.debug("Collected objectsToValidate", objectsToValidate)
        objectsToValidate.each {
            config.script.sh "kubedog --kube-config \$KUBECONFIG -n ${namespace} rollout track ${it} 2>&1"
        }
    }

    String extractObject(String rawString) {
        logger.debug("got string", rawString)
        String extractedObject = rawString.substring(rawString.indexOf("/") + 1, rawString.indexOf(" "))
        logger.debug("extractedObject", extractedObject)
        return extractedObject
    }

    String unsetEnvServiceDiscovery() {
/*
        String envsToUnset = ""
        String currentEnv = shWithOutput(script, "printenv")
        currentEnv.split("\n").findAll { it ==~ ~/.+(_SERVICE_|_PORT).+/ }.each {
            envsToUnset += "unset ${it.tokenize("=")[0]}\n"
        }
        log.trace("envsToUnset:\n $envsToUnset")
        return envsToUnset
        ^^^ seems, this realization has a bug which cause of error - Error java.io.NotSerializableException: org.codehaus.groovy.util.ArrayIterator
*/
        return 'for i in \$(set | grep "_SERVICE_\\|_PORT" | cut -f1 -d=); do unset \$i; done'
    }
}

package com.nextiva.tools.deploy

import com.nextiva.environment.Environment
import com.nextiva.tools.ToolFactory
import hudson.AbortException

import static com.nextiva.SharedJobsStaticVars.VAULT_URL
import static com.nextiva.config.Global.instance as global
import static com.nextiva.utils.GitUtils.clone
import static com.nextiva.utils.Utils.shWithOutput

class Repository {
    String path
    String repository
    String branch

    Repository(String path, String repository, String branch) {
        this.path = path
        this.repository = repository
        this.branch = branch
    }
}

class Kubeup extends DeployTool {

    String kubeUpHome
    Repository cloudApps
    Repository cloudPlatform

    Kubeup(Script script, Map deployToolConfig) {
        super(script, deployToolConfig)
        this.kubeUpHome = "$toolHome/kubeup"
        this.cloudApps = new Repository("$toolHome/cloud-apps",
                deployToolConfig.get("cloudAppsRepository"),
                deployToolConfig.get("cloudAppsBranch"))
        this.cloudPlatform = new Repository("$toolHome/cloud-platform",
                deployToolConfig.get("cloudPlatformRepository"),
                deployToolConfig.get("cloudPlatformBranch"))
    }

    @Override
    void deploy(Environment environment) {
        init(environment.kubernetesCluster)
        logger.info("Start deploy cloudApp: ${global.appName} , version: ${global.globalVersion}, namespace: ${environment.kubernetesNamespace}, configset: ${environment.kubernetesConfigSet}")

        logger.info('Checking of application manifests ...')
        install(global.appName, global.globalVersion, environment.kubernetesNamespace, environment.kubernetesConfigSet, true)
        logger.info('Deploying application into Kubernetes ...')
        install(global.appName, global.globalVersion, environment.kubernetesNamespace, environment.kubernetesConfigSet, false)
        println("this is kubernetes deployment" + toString())
    }

    void init(String clusterDomain) {
        logger.debug("start init $name tool")

        logger.debug("Clonning repository $repository branch $branch into $kubeUpHome")
        clone(script, repository, branch, kubeUpHome)
        logger.debug("clone complete")

        logger.debug("Clonning repository $cloudApps.repository branch $cloudApps.branch into $cloudApps.path")
        clone(script, cloudApps.repository, cloudApps.branch, cloudApps.path)
        logger.debug("clone complete")

        logger.debug("Clonning repository $cloudPlatform.repository branch $cloudPlatform.branch into $cloudPlatform.path")
        clone(script, cloudPlatform.repository, cloudPlatform.branch, cloudPlatform.path)
        logger.debug("clone complete")

        script.container(name) {
            script.dir(toolHome) {
                script.env.PATH = "${script.env.PATH}:${toolHome}"
                kubectlInstall()
                kubeupInstall()
                kubedogInstall()
                jqInstall()
                vaultInstall()
                vaultLogin(VAULT_URL)
            }
            kubeLogin(clusterDomain)
        }
        logger.debug("init complete")
        initialized = true
    }

    def kubeupInstall() {
        logger.debug("kubeupInstall start")
        try {
            script.sh "kubeup --version"
        } catch (e) {
            throw new AbortException("kubeup is not installed, aborting... $e")
        }
        logger.debug("kubeupInstall complete")
    }

    def kubedogInstall() {
        logger.debug("kubedogInstall start")
        try {
            String output = shWithOutput(script, "kubedog version")
            logger.debug("$output")
        } catch (e) {
            logger.warn("kubedog is not installed, going to install kubedog...")
            String out = shWithOutput(script, """
            curl -L https://dl.bintray.com/flant/kubedog/v0.2.0/kubedog-linux-amd64-v0.2.0 -o $toolHome/kubedog
            chmod +x $toolHome/kubedog
            kubedog version""")
            logger.debug("$out")
            script.env.KUBEDOG_KUBE_CONFIG = "${toolHome}/kubeconfig"
        }
        logger.debug("kubedogInstall complete")
    }

    def kubectlInstall() {
        logger.debug("kubectlInstall start")
        script.kubernetes.kubectlInstall()
        logger.debug("kubectlInstall complete")
    }

    def vaultInstall() {
        logger.debug("vaultInstall start")
        script.kubernetes.vaultInstall()
        logger.debug("vaultInstall complete")
    }

    def jqInstall() {
        logger.debug("jqInstall start")
        script.kubernetes.jqInstall()
        logger.debug("jqInstall complete")
    }

    def kubeloginInstall() {
        logger.debug("going to install kubelogin")
        //TODO: add kubelogin install method
//            script.kubernetes.kubeloginInstall()
        logger.debug("kubelogin complete")
        logger.debug("setting env variables")
        script.env.KUBELOGIN_CONFIG = "${toolHome}/.kubelogin"
        script.env.KUBECONFIG = "${toolHome}/kubeconfig"
    }

    def kubeLogin(String clusterDomain) {
        kubeloginInstall()
        script.withCredentials([script.usernamePassword(credentialsId: 'jenkinsbitbucket', usernameVariable: 'KUBELOGIN_USERNAME', passwordVariable: 'KUBELOGIN_PASSWORD')]) {
            String output = shWithOutput(script, """
            unset KUBERNETES_SERVICE_HOST
            kubelogin -s login.${clusterDomain} 2>&1
            kubectl get nodes 2>&1
            """)
            logger.debug("Kubelogin output", output)
        }
    }

    def vaultLogin(String vaultUrl) {
        logger.debug("trying to login in the Vault")
        script.withCredentials([script.usernamePassword(credentialsId: "vault-ro-access", usernameVariable: 'VAULT_RO_USER', passwordVariable: 'VAULT_RO_PASSWORD')]) {
            try {
                String output = shWithOutput(script, "vault login -method=ldap -no-print -address ${vaultUrl} username=${script.env.VAULT_RO_USER} password=${script.env.VAULT_RO_PASSWORD}")
                logger.trace("Vault login output", output)
            } catch (e) {
                logger.error("Error! Got an error trying to initiate the connect with Vault \n output $output", e)
                throw new AbortException("Error! Got an error trying to initiate the connect with Vault ${vaultUrl}")
            }
        }
        logger.debug("Vault login complete")
    }

    def install(String application, String version, String namespace, String configset, Boolean dryRun = true) {
        logger.debug("Install application: $application, version: $version, namespace: $namespace, configset: $configset, dryRun = $dryRun")
        String output = ""
        try {
            script.container(name) {
                script.dir(toolHome) {
                    String dryRunParam = dryRun ? '--dry-run' : ''
                    output = shWithOutput(script, """
                    cd "\$(find cloud-apps/apps/ cloud-platform/apps -maxdepth 1 -type d -name $application | head -1)/../../"
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
            throw new AbortException("kubeup install failure... installOutput: >>> \n $output \n")
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
            script.sh "kubedog --kube-config ${toolHome}/kubeconfig -n ${namespace} rollout track ${it} 2>&1"
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

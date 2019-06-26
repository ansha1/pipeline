package com.nextiva.tools.deploy

import hudson.AbortException

import static com.nextiva.SharedJobsStaticVars.VAULT_URL
import static com.nextiva.utils.GitUtils.clone
import static com.nextiva.utils.Utils.shWithOutput

class Kubeup extends DeployTool {
    Kubeup(Script script, Map deployToolConfig) {
        super(script, deployToolConfig)
    }

    Boolean deploy(String cloudApp, String version, String namespace, String configset) {
        if (!isInitialized()) {
            log.error("Kubeup is not initialized, aborting...")
            throw new AbortException("Kubeup is not installed, aborting...")
        }
        log.info("Start deploy cloudApp: $cloudApp , version: $version, namespace: $namespace, configset: $configset")

        log.info('Checking of application manifests ...')
        install(cloudApp, version, namespace, configset, true)
        log.info('Deploying application into Kubernetes ...')
        install(cloudApp, version, namespace, configset, false)
        println("this is kubernetes deployment" + toString())
        return true
    }

    void init(String clusterDomain) {
        log.debug("start init $name tool")
        log.debug("Clonning repository $repository branch $branch in toolHome $toolHome")
        clone(script, repository, branch, toolHome)
        log.debug("clone complete")
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
        log.debug("init complete")
        initialized = true
    }

    def kubeupInstall() {
        log.debug("kubeupInstall start")
        try {
            script.sh "kubeup --version"
        } catch (e) {
            throw new AbortException("kubeup is not installed, aborting... $e")
        }
        log.debug("kubeupInstall complete")
    }

    def kubedogInstall() {
        log.debug("kubedogInstall start")
        try {
            String output = shWithOutput(script, "kubedog version")
            log.debug("$output")
        } catch (e) {
            log.warn("kubedog is not installed, going to install kubedog...")
            String out = shWithOutput(script, """
            curl -L https://dl.bintray.com/flant/kubedog/v0.2.0/kubedog-linux-amd64-v0.2.0 -o $toolHome/kubedog
            chmod +x $toolHome/kubedog
            kubedog version""")
            log.debug("$out")
            script.env.KUBEDOG_KUBE_CONFIG = "${toolHome}/kubeconfig"
        }
        log.debug("kubedogInstall complete")
    }

    def kubectlInstall() {
        log.debug("kubectlInstall start")
        script.kubernetes.kubectlInstall()
        log.debug("kubectlInstall complete")
    }

    def vaultInstall() {
        log.debug("vaultInstall start")
        script.kubernetes.vaultInstall()
        log.debug("vaultInstall complete")
    }

    def jqInstall() {
        log.debug("jqInstall start")
        script.kubernetes.jqInstall()
        log.debug("jqInstall complete")
    }

    def kubeloginInstall() {
        log.debug("going to install kubelogin")
        //TODO: add kubelogin install method
//            script.kubernetes.kubeloginInstall()
        log.debug("kubelogin complete")
        log.debug("setting env variables")
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
            log.debug("Kubelogin output", output)
        }
    }

    def vaultLogin(String vaultUrl) {
        log.debug("trying to login in the Vault")
        script.withCredentials([script.usernamePassword(credentialsId: "vault-ro-access", usernameVariable: 'VAULT_RO_USER', passwordVariable: 'VAULT_RO_PASSWORD')]) {
            try {
                String output = shWithOutput(script, "vault login -method=ldap -no-print -address ${vaultUrl} username=${script.env.VAULT_RO_USER} password=${script.env.VAULT_RO_PASSWORD}")
                log.trace("Vault login output", output)
            } catch (e) {
                log.error("Error! Got an error trying to initiate the connect with Vault \n output $output", e)
                throw new AbortException("Error! Got an error trying to initiate the connect with Vault ${vaultUrl}")
            }
        }
        log.debug("Vault login complete")
    }

    def install(String cloudApp, String version, String namespace, String configset, Boolean dryRun = true) {
        log.debug("Install cloudApp: $cloudApp , version: $version, namespace: $namespace, configset: $configset, dryRun = $dryRun")
        String output = ""
        try {
            script.container(name) {
                script.dir(toolHome) {
                    String dryRunParam = dryRun ? '--dry-run' : ''
                    output = shWithOutput(script, """
                    # fix for builds running in kubernetes, clean up predefined variables.
                    ${unsetEnvServiceDiscovery()}
                    BUILD_VERSION=${version}
                    kubeup --yes --no-color ${dryRunParam} --namespace ${namespace} --configset ${configset} ${cloudApp} 2>&1
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
        log.debug("find all kubernetes objects in the cloudapp in order to validate", installOutput)
        log.debug("==========================================================================================")

        List objectsToValidate = []
        installOutput.split("\n").each {
            log.trace("parse object $it")
            switch (it) {
                case ~/^(deployment.apps|javaapp.nextiva.io|pythonapp.nextiva.io).+$/:
                    log.trace("Found k8s object $it")
                    objectsToValidate.add("deployment ${extractObject(it)}")
                    break
                case ~/^statefulset.apps.+$/:
                    log.trace("Found k8s object $it")
                    objectsToValidate.add("statefulset ${extractObject(it)}")
                    break
                case ~/^daemonset.extentions.+$/:
                    log.trace("Found k8s object $it")
                    objectsToValidate.add("daemonset ${extractObject(it)}")
                    break
                case ~/^job.batch.+$/:
                    log.trace("Found k8s object $it")
                    objectsToValidate.add("job ${extractObject(it)}")
                    break
            }
        }
        log.debug("Collected objectsToValidate", objectsToValidate)
        objectsToValidate.each {
            script.sh "kubedog --kube-config ${toolHome}/kubeconfig -n ${namespace} rollout track ${it} 2>&1"
        }
    }

    String extractObject(String rawString) {
        log.debug("got string", rawString)
        String extractedObject = rawString.substring(rawString.indexOf("/") + 1, rawString.indexOf(" "))
        log.debug("extractedObject", extractedObject)
        return extractedObject
    }

    String unsetEnvServiceDiscovery() {
        String envsToUnset = ""
        String currentEnv = shWithOutput(script, "printenv")
        currentEnv.split("\n").findAll { it ==~ ~/.+(_SERVICE_|_PORT).+/ }.each {
            envsToUnset += "unset ${it.tokenize("=")[0]}\n"
        }
        log.trace("envsToUnset:\n $envsToUnset")
        return envsToUnset
    }
}

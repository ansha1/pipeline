package com.nextiva.tools.deploy

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.nextiva.SharedJobsStaticVars

import static com.nextiva.utils.GitUtils.clone
import static com.nextiva.utils.Utils.shWithOutput

class Kubeup extends DeployTool {
    Kubeup(Script script, Map configuration) {
        super(script, configuration)
    }

    Boolean deploy(String cloudApp, String version, String namespace, String configset) {
        log.info("Start deploy cloudApp: $cloudApp , version: $version, namespace: $namespace, configset: $configset")

        vaultLogin(SharedJobsStaticVars.VAULT_URL)
        log.info('Checking of application manifests ...')
        install(cloudApp, version, namespace, configset, true)
        log.info('Deploying application into Kubernetes ...')
        install(cloudApp, version, namespace, configset, false)
        println("this is kubernetes deployment" + toString())
        return true
    }

    void init(String clusterDomain) {
        log.debug("start init ${getName()} tool")
        log.debug("Clonning repository $repository branch $branch in toolHome $toolHome")
        clone(script, repository, branch, toolHome)
        log.debug("clone complete")
        script.container(getName()) {
            script.dir(toolHome) {
                kubectlInstall()
                kubeupInstall()
                vaultInstall()
                jqInstall()
                kubeloginInstall()
            }
            kubeLogin(clusterDomain)
        }
        log.debug("init complete")
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
//            script.kubernetes.kubeloginInstall()
        log.debug("kubelogin complete")
        log.debug("setting env variables")
        script.env.KUBELOGIN_CONFIG = "${toolHome}/.kubelogin"
        script.env.KUBECONFIG = "${toolHome}/kubeconfig"
        script.env.PATH = "${script.env.PATH}:${toolHome}"
    }

    def kubeLogin(String clusterDomain) {
        script.withCredentials([usernamePassword(credentialsId: 'jenkinsbitbucket', usernameVariable: 'KUBELOGIN_USERNAME', passwordVariable: 'KUBELOGIN_PASSWORD')]) {
            script.sh(script: """
            unset KUBERNETES_SERVICE_HOST
            kubelogin -s login.${clusterDomain} 2>&1
            kubectl get nodes
            """)
        }
    }

    def vaultLogin(String vaultUrl) {
        log.debug("trying to login in the Vault")
        script.withCredentials([usernamePassword(credentialsId: "vault-ro-access", usernameVariable: 'VAULT_RO_USER', passwordVariable: 'VAULT_RO_PASSWORD')]) {
            try {
                sh "vault login -method=ldap -no-print -address ${vaultUrl} username=${VAULT_RO_USER} password=${VAULT_RO_PASSWORD}"
            } catch (e) {
                log.error("Error! Got an error trying to initiate the connect with Vault")
                error("Error! Got an error trying to initiate the connect with Vault ${vaultUrl}")
            }
        }
        log.debug("Vault login complete")
    }

    def install(String cloudApp, String version, String namespace, String configset, Boolean dryRun = true) {
        script.dir(toolHome) {
            //TODO: change this to the --dry-run=true or --dry-run=false
            String dryRunParam = dryRun ? '--dry-run' : ''
            String installOutput = shWithOutput(script, """
              # fix for builds running in kubernetes, clean up predefined variables.
              for i in \$(set | grep "_SERVICE_\\|_PORT" | cut -f1 -d=); do unset \$i; done
              BUILD_VERSION=${version}
              kubeup --yes --no-color ${dryRunParam} --namespace ${namespace} --configset ${configset} ${cloudApp} 2>&1
              """)
            validate(installOutput, namespace)
        }
    }

    def validate(String installOutput, String namespace) {
        log.debug("find all kubernetes objects in the cloudapp in order to validate", installOutput)

        Multimap objectsToValidate = ArrayListMultimap.create()
        installOutput.split("\n").each {
            switch (it) {
                case it.startsWith("deployment.apps"):
                    objectsToValidate.put("deployment", extractObject(it))
                    break
                case it.startsWith("javaapp.nextiva.io"):
                    objectsToValidate.put("deployment", extractObject(it))
                    break
                case it.startsWith("pythonapp.nextiva.io"):
                    objectsToValidate.put("deployment", extractObject(it))
                    break
                case it.startsWith("statefulset.apps"):
                    objectsToValidate.put("statefulset", extractObject(it))
                    break
                case it.startsWith("daemonset.extentions"):
                    objectsToValidate.put("daemonset", extractObject(it))
                    break
                case it.startsWith("job.batch"):
                    objectsToValidate.put("job", extractObject(it))
                    break
            }
        }

        objectsToValidate.entries().each { type, name ->
            shWithOutput(script, "kubedog --kube-config = ${toolHome}/kubeconfig -n ${namespace} rollout track ${type} ${name}")
        }
    }

    String extractObject(String rawString) {
        log.debug("got string", rawString)
        String extractedObject = rawString.substring(rawString.indexOf("/"), rawString.indexOf(" "))
        log.debug("extractedObject", extractedObject)
        return extractedObject
    }
}

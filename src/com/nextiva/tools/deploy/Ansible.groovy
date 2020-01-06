package com.nextiva.tools.deploy

import com.nextiva.environment.Environment
import com.nextiva.tools.build.*

import static com.nextiva.SharedJobsStaticVars.ANSIBLE_PASSWORD_PATH
import static com.nextiva.SharedJobsStaticVars.GIT_CHECKOUT_CREDENTIALS
import static com.nextiva.config.Config.instance as config
import static com.nextiva.utils.GitUtils.clone

class Ansible extends DeployTool {

    String inventoryPath
    String playbookPath

    Ansible(Map<String, String> configuration) {
        super(configuration)
        inventoryPath = configuration.get('inventoryPath').trim().replaceAll(/\/$/, "")
        playbookPath = configuration.get('playbookPath').trim()
    }

    void init() {
        if (isInitialized()) {
            return
        }
        logger.debug("start init $name tool")
        logger.debug("Clonning repository $repository branch $branch in toolHome $toolHome")
        clone(config.script, repository, branch, toolHome)
        logger.debug("clone complete")
        logger.debug("init complete")
        initialized = true
    }

    @Override
    void deploy(Environment environment) {
        Script s = config.script
        s.container(name) {
            init()
            s.stage("ansible: ${environment.name}") {
                s.withCredentials([s.file(credentialsId: 'ansible-vault-password-release-management',
                        variable: 'ANSIBLE_PASSWORD_PATH')]) {
                    s.sh "ln -s \$ANSIBLE_PASSWORD_PATH ${ANSIBLE_PASSWORD_PATH}"
                    s.sshagent(credentials: [GIT_CHECKOUT_CREDENTIALS]) {
                        s.runAnsiblePlaybook(toolHome, "${inventoryPath}/${environment.ansibleInventory}",
                                playbookPath, getAnsibleExtraVars())
                    }
                }
            }
            if (environment.healthChecks) {
                s.stage("healthcheck: $environment.name") {
                    health(environment.healthChecks)
                }
            }
        }
    }


    Map getAnsibleExtraVars() {

        Map vars = [:]
        BuildTool buildTool = config.build[0].instance
        switch (buildTool.class) {
            case Maven:
                vars = ['application_version': config.version,
                        'maven_repo'         : config.version.contains('SNAPSHOT') ? 'snapshots' : 'releases']
                break
            case Python:
                vars = ['version': config.version]
                break
            case Npm:
                vars = ['version'            : config.version,
                        'component_name'     : config.appName,
                        'static_assets_files': config.appName]
                break
            default:
                error("Incorrect programming language, please set one of the supported languages: java, python, js")
                break
        }
        return vars
    }
}


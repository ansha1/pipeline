package com.nextiva.tools.deploy

import com.nextiva.environment.Environment

import static com.nextiva.config.Config.instance as config
import static com.nextiva.utils.GitUtils.clone

class Ansible extends DeployTool {

    Ansible(Map configuration) {
        super(configuration)
    }

//    Map deployment = ["name"         : "Ansible",
//                      "image"        : "ansibleimage",
//                      "repository"   : "repo",
//                      "branch"       : "master",
//                      "inventoryPath": 'ansible/role-based_playbooks/inventory/java-app/dev',
//                      "playbookPath" : 'ansible/role-based_playbooks/java-app.yml',
//                      "ansibleArgs"  : 'args']
    void init() {
        logger.debug("start init $name tool")
        logger.debug("Clonning repository $repository branch $branch in toolHome $toolHome")
        clone(config.script, repository, branch, toolHome)
        logger.debug("clone complete")
        logger.debug("init complete")
        initialized = true
    }

    @Override
    void deploy(Environment environment) {
        config.script.stage("ansible: $it.environmentName") {
            config.script.container(getName()) {
                config.script.runAnsiblePlaybook(repoDir, "$it.ansibleInventoryPath/$it.ansibleInventory", it.ansiblePlaybookPath, getAnsibleExtraVars(configuration))
            }
        }
        config.script.stage("healthcheck: $it.environmentName") {
            health(it.healthChecks)
        }
    }


    Map getAnsibleExtraVars(Map configuration) {

        Map vars = [:]
        switch (configuration.get('language')) {
            case 'java':
                vars = ['application_version': configuration.get("buildVersion"),
                        'maven_repo'         : configuration.get("version").contains('SNAPSHOT') ? 'snapshots' : 'releases']
                break
            case 'python':
                vars = ['version': configuration.get("buildVersion")]
                break
            case 'js':
                vars = ['version'            : configuration.get("buildVersion"),
                        'component_name'     : configuration.get("appName"),
                        'static_assets_files': configuration.get("appName")]
                break
            default:
                error("Incorrect programming language, please set one of the supported languages: java, python, js")
                break
        }
        return vars
    }
}


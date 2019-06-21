package com.nextiva.tools.deploy

class Ansible extends DeployTool {

    Ansible(Script script, Map configuration) {
        super(script, configuration)
    }

//    Map deployment = ["name"         : "Ansible",
//                      "image"        : "ansibleimage",
//                      "repository"   : "repo",
//                      "branch"       : "master",
//                      "inventoryPath": 'ansible/role-based_playbooks/inventory/java-app/dev',
//                      "playbookPath" : 'ansible/role-based_playbooks/java-app.yml',
//                      "ansibleArgs"  : 'args']

    Boolean deploy() {
        environments.each {
            script.stage("ansible: $it.environmentName") {
                script.container("ansible") {
                    def repoDir = script.prepareRepoDir(repository, branch)
                    script.runAnsiblePlaybook(repoDir, "$it.ansibleInventoryPath/$it.ansibleInventory", it.ansiblePlaybookPath, getAnsibleExtraVars(configuration))
                }
            }
            stage("healthcheck: $it.environmentName") {
                health(it.healthChecks)
            }
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


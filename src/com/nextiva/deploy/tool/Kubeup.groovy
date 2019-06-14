package com.nextiva.deploy.tool

import com.nextiva.environment.Environment
import static com.nextiva.utils.GitUtils.clone

class Kubeup extends DeployTool {
    Kubeup(Script script, List<Environment> environments, Map configuration) {
        super(script, environments, configuration)
    }

    @Override
    Boolean deploy() {
        println("this is kubernetes deployment" + toString())
        return true
    }

    @Override
    void init() {
        log.debug("start init ${getName()} tool")
        String toolHome = getToolHome()
        log.debug("Clonning repository $repository branch $branch in toolHome $toolHome")
        clone(script, repository, branch, toolHome)
        log.debug("clone complete")
        script.dir(toolHome){
            script.kubernetes.kubectlInstall()
            script.kubernetes.vaultInstall()
        }

    }
}

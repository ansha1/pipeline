package com.nextiva.stages

import com.nextiva.stages.BasicStage

class SecurityScan extends BasicStage {
    protected SecurityScan(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
//           securityScan()
            //veracode
            //tennable
            //sourceClear
        }
    }
}

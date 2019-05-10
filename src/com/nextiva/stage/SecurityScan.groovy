package com.nextiva.stage

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

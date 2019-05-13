package com.nextiva.stage

class SecurityScan extends BasicStage {
    protected SecurityScan(script, configuration) {
        super(script, configuration)
    }

    def execute(){
        script.stage(this.getClass().getSimpleName()) {
            script.print("This is execuiton of ${this.getClass().getSimpleName()} stage")
//           securityScan()
            //veracode
            //tennable
            //sourceClear
        }
    }
}

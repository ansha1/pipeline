package com.nextiva.stages.stage

class SecurityScan extends Stage {
    SecurityScan(Script script, Map configuration) {
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

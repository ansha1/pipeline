package com.nextiva.stages.stage

class CollectBuildResults extends Stage {
    CollectBuildResults(Script script, Map configuration) {
        super(script, configuration)
    }

    def stageBody() {
       log.info("Collecting logs from containers")
        //code
       log.info("Collecting JUnit results")
        //code
       log.info("Publish to S3")
    }
}

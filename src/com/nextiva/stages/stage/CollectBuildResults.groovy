package com.nextiva.stages.stage

class CollectBuildResults extends Stage {
    CollectBuildResults(Script script, Map configuration) {
        super(script, configuration)
    }

    def stageBody() {
       logger.info("Collecting logs from containers")
        //code
       logger.info("Collecting JUnit results")
        //code
       logger.info("Publish to S3")
    }
}

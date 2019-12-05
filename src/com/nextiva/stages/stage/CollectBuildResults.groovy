package com.nextiva.stages.stage

import static com.nextiva.config.Config.instance as config

class CollectBuildResults extends Stage {
    CollectBuildResults() {
        super()
    }

    def stageBody() {
       logger.info("Collecting logs from containers")
        //code
       logger.info("Collecting JUnit results")
        //code
       logger.info("Publish to S3")
    }
}

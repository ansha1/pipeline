import com.nextiva.config.Config
import com.nextiva.stages.stage.Stage

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    Config config = new Config(this, pipelineParams)
    kubernetesSlave(config.getSlaveConfiguration()) {
        pipelineExecution(config.getStages(), config.getJobTimeoutMinutes())
    }
}

void pipelineExecution(List<Stage> stages, String jobTimeoutMinutes) {

    //exclude steps than should be executed in the finally block
    Stage notify = flow.pop()
    Stage resultsCollector = flow.pop()

    try {
        timeout(time: jobTimeoutMinutes, unit: 'MINUTES') {
            stages.each {
                it.execute()
            }
        }
    } catch (t) {
        //some error handling
        currentBuild.result = "FAILED"
        log.error("this is  error $t")
    } finally {
        //test results collecting
        resultsCollector.execute()
        //slack notification
        notify.execute()
    }
}
import com.nextiva.config.Config
import com.nextiva.stages.stage.Stage
import com.nextiva.utils.LogLevel
import com.nextiva.utils.Logger


def call(body) {
    Logger.init(this, LogLevel.INFO)
    timestamps {
        ansiColor('xterm') {
            // evaluate the body block, and collect configuration into the object
            def pipelineParams = [:]
            body.resolveStrategy = Closure.DELEGATE_FIRST
            body.delegate = pipelineParams
            body()
            log.info("Starting Nextiva Pipeline")
            Config config = new Config(this, pipelineParams)
            config.configure()
            log.info("config creation complete")
            kubernetesSlave(config.getSlaveConfiguration()) {
                pipelineExecution(config.getStages(), config.getJobTimeoutMinutes())
            }
        }
    }
}

void pipelineExecution(List<Stage> stages, String jobTimeoutMinutes) {
    Logger log = new Logger(this)
    //exclude steps than should be executed in the finally block
    Stage notify = stages.pop()
    Stage resultsCollector = stages.pop()

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
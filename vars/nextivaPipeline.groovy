import com.nextiva.config.Config
import com.nextiva.stages.stage.Stage
import com.nextiva.utils.Logger
import hudson.model.Result


def call(body) {
    Logger.init(this, env.JOB_LOG_LEVEL)
    Logger logger = new Logger(this)
    timestamps {
        ansiColor('xterm') {
            // evaluate the body block, and collect configuration into the object
            def pipelineParams = [:]
            body.resolveStrategy = Closure.DELEGATE_FIRST
            body.delegate = pipelineParams
            body()
            logger.info("Starting Nextiva Pipeline")
            Config config = new Config(this, pipelineParams)
            config.configure()
            kubernetesSlave(config.getSlaveConfiguration()) {
                pipelineExecution(config.getStages(), config.getJobTimeoutMinutes())
            }
        }
    }
}

void pipelineExecution(List<Stage> stages, String jobTimeoutMinutes) {
    Logger logger = new Logger(this)
    //exclude steps than should be executed in the finally block
    Stage notify = stages.pop()
    Stage resultsCollector = stages.pop()

    try {
        timeout(time: jobTimeoutMinutes, unit: 'MINUTES') {
            stages.each {
                logger.debug("executing stage", it)
                it.execute()
            }
        }
    } catch (t) {
        //some error handling
        currentBuild.result = "FAILURE"
        currentBuild.rawBuild.result = Result.FAILURE
        logger.error("error in the pipeline execution", t)
    } finally {
        //test results collecting
        resultsCollector.execute()
        //slack notification
        notify.execute()
    }
}
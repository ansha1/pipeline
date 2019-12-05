import com.nextiva.config.PipelineConfig
import com.nextiva.config.Config
import com.nextiva.stages.stage.Stage
import com.nextiva.utils.Logger
import hudson.model.Result
import org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException


def call(body) {
    Logger.init(this, env.JOB_LOG_LEVEL)
    Logger logger = new Logger(this)
    timestamps {
        ansiColor('xterm') {
            // evaluate the body block, and collect configuration into the object
            PipelineConfig pipelineConfig = new PipelineConfig()
            body.resolveStrategy = Closure.DELEGATE_ONLY
            body.delegate = pipelineConfig
            pipelineConfig.script = this
            body()
            logger.info("Starting Nextiva Pipeline")

            Config config = new Config().getInstance()
            config.configure(pipelineConfig)
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
            stages.each { stage ->
                logger.debug("executing stage", stage)
                stage.execute()
            }
        }
    } catch (t) {
        currentBuild.result = "FAILURE"
        currentBuild.rawBuild.result = Result.FAILURE
        logger.error("error in the pipeline execution", t)
        if (t instanceof RejectedAccessException) {
            //test results collecting
            resultsCollector.execute()
            //slack notification
            notify.execute()
            throw t
        }
    }
    finally {
        //test results collecting
        resultsCollector.execute()
        //slack notification
        notify.execute()
    }
}
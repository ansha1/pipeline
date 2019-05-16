import com.nextiva.config.Config
import com.nextiva.stages.StageFactory
import com.nextiva.stages.stage.Stage

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    Config config = new Config(this, pipelineParams)

    List<Stage> flow = StageFactory.getStagesFromConfiguration(this, config.getConfiguration())

    Stage notificationSender = flow.pop()
    Stage resultsCollector = flow.pop()

    kubernetesSlave(configuration) {
        try {
            flow.each {
                it.execute()
            }
        } catch (t) {
            //some error handeling
            currentBuild.result = "FAILED"
            log.error("this is  error $t")
        } finally {
            //test results collecting
            resultsCollector.execute()
            //slack notification
            notificationSender.execute()
        }
    }
}
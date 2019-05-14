import com.nextiva.config.Config
import com.nextiva.stage.StageFactory
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    Config config = new Config(this, pipelineParams)

    Map configuration = config.getConfiguration()

    List flow = StageFactory.getStagesFromConfiguration(this, configuration)

    kubernetesSlave(configuration) {
        try {
            flow.each {
                it.execute()
            }
        } catch(t){
            //some error handeling
            currentBuild.result = "FAILED"
            log.error("this is  error $t")
        } finally {
            //test results collecting
            println("test results has been collected")
            //slack notification
            println("slack notification about build")
        }
    }
}
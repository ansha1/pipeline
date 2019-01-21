def call(Map Config) {
    stage("Checkout") {
        container(jnlp) {

        }
    }
    stage("Build version verification") {
        container(build) {

        }
    }
    stage("Unit testing") {
        container(build) {

        }
    }
    stage("Sonar analysing") {
        container(build) {

        }
    }
    stage("Build artifact") {
        container(build) {

        }
    }
    stage("Build docker image") {
        container(build) {

        }
    }
    stage("Publish artifact") {
        container(build) {

        }
    }
    stage("Publish docker image") {
        container(build) {

        }
    }
    stage("Veracode security scan") {
        container(jnlp) {

        }
    }
    stage("Tennable security scan") {
        container(build) {

        }
    }
    stage("Create integration test environment") {
        container(kubernetes) {

        }
    }
    stage("Run integration tests") {
        container(build) {

        }
    }
    stage("Deploy to the environment") {
        container(kubernetes) {

        }
    }
    stage("Post deploy step") {
        container(build) {

        }
        //Newrelic info
        //Prometheus build info
    }
    stage("Service healthcheck and version validation") {
        container(jnlp) {

        }
        //Newrelic info
        //Prometheus build info
    }
    stage("Post deploy stage") {
        container(build) {

        }
        //Newrelic info
        //Prometheus build info
    }
    stage("QA core team tests") {
        container(jnlp) {

        }
        //Newrelic info
        //Prometheus build info
    }
    stage("Send notifications") {
        container(jnlp) {

        }
    }

}
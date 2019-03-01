


def call(Map Config) {





    stage("Checkout") {
        container(jnlp) {
            checkout scm
        }
    }
    stage("Build version verification") {   //if on DEV,develop,master......
        container(build) {
checkversion(config)

            config.check
            if ()
        }
    }
    stage("Unit testing") {  //Dev release PR    master if trunk base
        container(build) {

        }
    }

    stage("Sonar analysing") {   //Dev brannh only
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
    stage("Integration tests") {  //only on the PR branch
        container(kubernetes) {

        }
    }
    stage("Deploy to the environment") {  //Only on dev/develop/release/master/hotfix
        container(kubernetes) {

        }
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
    return this
}
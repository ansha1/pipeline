def call(String jobName = '') {
    try {
       return jenkins.model.Jenkins.instance.getItem(jobName) != null
    } catch (e) {
        echo e.message
    }
    return false
}

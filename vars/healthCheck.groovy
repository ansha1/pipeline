def call(String healthCheckUrl, Integer timeLimit=5) {
    timeout(timeLimit) {   //default 5 minutes for starting
        waitUntil {
            try {
                httpRequest url: healthCheckUrl, consoleLogResponseBody: true
                return true
            } catch (e) {
                return false
            }
        }
    }
}


def list(List healthCheckUrls, Integer timeLimit=5){
    try {
        healthCheckUrls.each{healthCheck(it, timeLimit)}
    }
    catch (e) {
        error('Service startup failed ' + e)
    }
}

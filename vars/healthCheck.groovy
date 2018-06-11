def call(String healthcheckUrl, Integer time_limit=2) {
    timeout(time_limit) {   //default 2 minutes for starting
        waitUntil {
            try {
                httpRequest url: healthcheckUrl, consoleLogResponseBody: true
                return true
            } catch (e) {
                return false
            }
        }
    }
}


def list(List healthcheckUrls, Integer time_limit=2){
    try {
        healthcheckUrls.each{healthCheck(it, time_limit)}
    }
    catch (e) {
        error('Service startup failed ' + e)
    }
}

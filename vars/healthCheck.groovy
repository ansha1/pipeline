def call(String healthcheckUrl, Integer time=2) {
    timeout(time) {   //default 2 minutes for starting
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

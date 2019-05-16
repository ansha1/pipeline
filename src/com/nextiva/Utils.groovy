package com.nextiva

class Utils {

    static String shWithOutput(script, String command) {
        return script.sh(
                script: command,
                returnStdout: true
        ).trim()
    }
}

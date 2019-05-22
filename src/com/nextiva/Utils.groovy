package com.nextiva

class Utils {

    static String shWithOutput(script, String command) {
        return script.sh(
                script: command,
                returnStdout: true
        ).trim()
    }

    static void shOrClosure(script, def command) {
        if (command instanceof Closure){
            command()
        }else{
            shWithOutput(script, command)
        }
    }
}

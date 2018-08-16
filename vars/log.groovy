import groovy.transform.Field
import org.codehaus.groovy.runtime.StackTraceUtils
import static com.nextiva.SharedJobsStaticVars.*


@Field
static final END_CHAR = '\033[0m'


def call(String message) {
    print(message.toString())
}

Boolean isDebug() {
    return params.DEBUG || new Boolean("${env.DEBUG}")
}

def info(message) {
    def list = message.toString().readLines()
    list.each{blueBold("[INFO] " + it)}
}

def warning(message) {
    def list = message.toString().readLines()
    list.each{yellowBold("[WARNING] " + it)}
}

def warn(message){
    warning(message)
}

def error(message) {
    def list = message.toString().readLines()
    list.each{redBold("[ERROR] " + it)}
}

def debug(message) {
    if(isDebug()){
        def list = message.toString().readLines()
        list.each{magnetaBold("[DEBUG] " + it)}
    }
}

def deprecated(message) {
    def list = message.toString().readLines()
    list.each{magnetaBold("[DEPRECATED] " + it)}

    String upstreamMethod = StackTraceUtils.sanitize(new Throwable()).stackTrace[1].methodName
    prometheus.sendGauge('deprecated', PROMETHEUS_DEFAULT_METRIC, [upstream_method: upstreamMethod, message: message])
}

def blue(String message) {
    print("\033[34m${message}${END_CHAR}")
}

def green(String message) {
    print("\033[32m${message}${END_CHAR}")
}

def yellow(String message) {
    print("\033[33m${message}${END_CHAR}")
}

def red(String message) {
    print("\033[31m${message}${END_CHAR}")
}

def magneta(String message) {
    print("\033[35m${message}${END_CHAR}")
}

def blueBold(String message) {
    print("\033[1;34m${message}${END_CHAR}")
}

def greenBold(String message) {
    print("\033[1;32m${message}${END_CHAR}")
}

def yellowBold(String message) {
    print("\033[1;33m${message}${END_CHAR}")
}

def redBold(String message) {
    print("\033[1;31m${message}${END_CHAR}")
}

def magnetaBold(String message) {
    print("\033[1;35m${message}${END_CHAR}")
}

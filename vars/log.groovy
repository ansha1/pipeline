import groovy.transform.Field


@Field
static final END_CHAR = '\033[0m'


def call(String message) {
    print(message)
}

def isDebug(){
    return params.DEBUG || new Boolean("${env.DEBUG}")
}

def multiline(String prefix, String message) {
    def list = message.readLines()
    def result = ''

    list.eachWithIndex{line, index ->
        result = "${result}${prefix}${line}"
        if(index < list.size() - 1){
            result = result + "\n"
        }
    } 
    return result
}

def info(message) {
    blueBold( multiline("[INFO] ", message) )
}

def warning(message) {
    yellowBold( multiline("[WARNING] ", message) )
}

def error(message) {
    redBold( multiline("[ERROR] ", message) )
}

def debug(message) {
    if(isDebug()){
        magnetaBold( multiline("[DEBUG] ", message) )
    }
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

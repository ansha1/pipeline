import groovy.transform.Field


@Field
static final END_CHAR = '\033[0m'


def call(String message) {
    print(message)
}

def info(message) {
    blueBold("[INFO] " + message)
}

def warning(message) {
    yellowBold("[WARNING] " + message)
}

def error(message) {
    redBold("[ERROR] " + message)
}

def debug(message) {
    if(params.DEBUG || new Boolean("${env.DEBUG}")){
        magnetaBold("[DEBUG] " + message)
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

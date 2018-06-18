import groovy.transform.Field


@Field
static final END_CHAR = '\033[0m'


def call(String message) {
    print(message)
}

def printColor(String message) {
    ansiColor('xterm') {
        print(message)
    }
}

def info(message) {
    blueBold(message)
}

def warning(message) {
    yellowBold(message)
}

def error(message) {
    redBold(message)
}

def debug(message) {
    if(params.DEBUG || new Boolean(env.DEBUG){
        magnetaBold(message)
    }
}

def blue(String message) {
    printColor("\033[34m${message}${END_CHAR}")
}

def green(String message) {
    printColor("\033[32m${message}${END_CHAR}")
}

def yellow(String message) {
    printColor("\033[33m${message}${END_CHAR}")
}

def red(String message) {
    printColor("\033[31m${message}${END_CHAR}")
}

def magneta(String message) {
    printColor("\033[35m${message}${END_CHAR}")
}

def blueBold(String message) {
    printColor("\033[1;34m${message}${END_CHAR}")
}

def greenBold(String message) {
    printColor("\033[1;32m${message}${END_CHAR}")
}

def yellowBold(String message) {
    printColor("\033[1;33m${message}${END_CHAR}")
}

def redBold(String message) {
    printColor("\033[1;31m${message}${END_CHAR}")
}

def magnetaBold(String message) {
    printColor("\033[1;35m${message}${END_CHAR}")
}

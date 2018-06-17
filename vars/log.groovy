import groovy.transform.Field


@Field
static final  END_CHAR = '\033[0m '
@Field
static final  BLUE_BOLD = '\033[1;34m'
@Field
static final YELLOW_BOLD = '\033[1;33m'
@Field
static final RED_BOLD = '\033[1;31m'


def printAnsiColor(String message, String color) {
    ansiColor('xterm') {
        echo "${color}${message}${END_CHAR}"
    }
}

def info(message) {
    printAnsiColor(message, BLUE_BOLD)
}

def warning(message) {
    printColor(message, YELLOW_BOLD)
}

def error(message) {
    printColor(message, RED_BOLD)
}

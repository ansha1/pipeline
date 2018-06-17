import groovy.transform.Field


@Field
String END_CHAR = '\033[0m '

@Field
String BLUE_BOLD = '\033[1;34m'

class Color {
    static final YELLOW_BOLD = '\033[1;33m'
    static final RED_BOLD = '\033[1;31m'
}

def printAnsiColor(String message, String color) {
    ansiColor('xterm') {
        echo "${color}${msg}${END_CHAR}"
    }
}

def info(message) {
    printAnsiColor('This is a Info message', BLUE_BOLD)
}

def warning(message) {
    printColor(message, YELLOW_BOLD)
}

def error(message) {
    printColor(message, RED_BOLD)
}
